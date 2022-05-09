package space.kscience.dataforge.workspace.distributed

import io.ktor.utils.io.core.*
import io.lambdarpc.coding.Coder
import io.lambdarpc.coding.CodingContext
import io.lambdarpc.dsl.LibService
import io.lambdarpc.dsl.def
import io.lambdarpc.transport.grpc.Entity
import io.lambdarpc.transport.serialization.Entity
import io.lambdarpc.transport.serialization.RawData
import io.lambdarpc.utils.Address
import io.lambdarpc.utils.Port
import io.lambdarpc.utils.toSid
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.Global
import space.kscience.dataforge.context.gather
import space.kscience.dataforge.data.Data
import space.kscience.dataforge.data.DataSet
import space.kscience.dataforge.data.DataTree
import space.kscience.dataforge.data.Goal
import space.kscience.dataforge.data.NamedData
import space.kscience.dataforge.data.await
import space.kscience.dataforge.data.component1
import space.kscience.dataforge.data.component2
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.MetaSerializer
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.asName
import space.kscience.dataforge.workspace.Task
import space.kscience.dataforge.workspace.TaskResult
import space.kscience.dataforge.workspace.Workspace
import space.kscience.dataforge.workspace.wrapResult
import java.nio.charset.Charset
import kotlin.reflect.KType

/**
 * Workspace that exposes its tasks for remote clients.
 */
public class ServiceWorkspace(
    address: String = "localhost",
    port: Int? = null,
    override val context: Context = Global.buildContext("workspace".asName()),
    data: DataSet<*> = runBlocking { DataTree<Any> {} },
    override val targets: Map<String, Meta> = mapOf(),
) : Workspace, Closeable {

    override val data: TaskResult<*> = wrapResult(data, Name.EMPTY, Meta.EMPTY)

    override val tasks: Map<Name, Task<*>>
        get() = context.gather(Task.TYPE)

    private val service = LibService(serviceId, address, port) {
        execute of { name ->
            val res = produce(name, Meta.EMPTY)
            LazyDecodableDataSetAdapter(res)
        }
    }

    /**
     * Address this workspace is available on.
     */
    public val address: Address = Address(address)

    /**
     * Port this workspace is available on.
     */
    public val port: Port
        get() = service.port

    /**
     * Start [ServiceWorkspace] as a service.
     */
    public fun start(): Unit = service.start()

    /**
     * Await termination of the service.
     */
    public fun awaitTermination(): Unit = service.awaitTermination()

    /**
     * Shutdown service.
     */
    public fun shutdown(): Unit = service.shutdown()

    override fun close(): Unit = service.shutdown()

    public companion object {
        internal val serviceId = "d41b95b1-828b-4444-8ff0-6f9c92a79246".toSid()
        internal val execute by serviceId.def(NameCoder, DataSetCoder)
    }
}

private object NameCoder : Coder<Name> {
    override fun decode(entity: Entity, context: CodingContext): Name {
        require(entity.hasData()) { "Entity should contain data" }
        val string = entity.data.toString(Charset.defaultCharset())
        return Name.parse(string)
    }

    override fun encode(value: Name, context: CodingContext): Entity =
        Entity(RawData.copyFrom(value.toString(), Charset.defaultCharset()))
}

@Serializable
private data class DataPrototype(
    val meta: String,
    val data: String,
) {
    companion object {
        suspend fun <T : Any> of(data: Data<T>, serializer: KSerializer<in T>): DataPrototype {
            val meta = Json.encodeToString(MetaSerializer, data.meta)
            val string = Json.encodeToString(serializer, data.await())
            return DataPrototype(meta, string)
        }
    }
}

/**
 * Data class that represents serializable [DataSet].
 */
@Serializable
private data class DataSetPrototype(
    val data: Map<String, DataPrototype>,
)

/**
 * [DataSetPrototype] builder.
 */
private fun <T : Any> DataSet<T>.toPrototype(): DataSetPrototype = runBlocking {
    val serializer = serializer(dataType)
    val map = mutableListOf<Pair<String, DataPrototype>>()
    flowData().map { (name, data) ->
        name.toString() to DataPrototype.of(data, serializer)
    }.toList(map)
    DataSetPrototype(map.associate { it })
}

/**
 * Trivial [Data] implementation.
 */
private class SimpleData(
    override val type: KType,
    override val meta: Meta,
    val data: Any,
) : Data<Any> {
    override val dependencies: Collection<Goal<*>>
        get() = emptyList()

    override val deferred: Deferred<Any>
        get() = CompletableDeferred(data)

    override fun async(coroutineScope: CoroutineScope): Deferred<Any> = deferred
    override fun reset() = Unit
}

/**
 * Trivial named data implementation.
 */
private class SimpleNamedData(
    override val name: Name,
    override val data: Data<Any>,
) : NamedData<Any>, Data<Any> by data

/**
 * Represents [DataSet] that should be initialized before usage.
 */
internal interface LazyDecodableDataSet<T : Any> : DataSet<T> {
    fun finishDecoding(type: KType)
}

private class LazyDecodableDataSetAdapter<T : Any>(val dataSet: DataSet<T>) :
    LazyDecodableDataSet<T>, DataSet<T> by dataSet {
    override fun finishDecoding(type: KType) = Unit
}

/**
 * Trivial [LazyDecodableDataSet] implementation.
 */
private class SimpleDataSet(private val prototype: DataSetPrototype) : LazyDecodableDataSet<Any> {

    lateinit var type: KType
    lateinit var data: Map<Name, Pair<Meta, Any>>

    override fun finishDecoding(type: KType) {
        this.type = type
        val serializer = serializer(type)
        this.data = prototype.data
            .mapKeys { (name, _) -> Name.of(name) }
            .mapValues { (_, pair) ->
                val (meta, data) = pair
                Pair(
                    Json.decodeFromString(MetaSerializer, meta),
                    Json.decodeFromString(serializer, data)!!
                )
            }
    }

    override val dataType: KType
        get() = type

    override fun flowData(): Flow<NamedData<Any>> =
        data.map { (name, pair) ->
            val (meta, data) = pair
            val wrapped = SimpleData(dataType, meta, data)
            SimpleNamedData(name, wrapped)
        }.asFlow()

    override suspend fun getData(name: Name): Data<Any>? = data[name]?.let { (meta, data) ->
        SimpleData(dataType, meta, data)
    }
}

private object DataSetCoder : Coder<LazyDecodableDataSet<Any>> {
    override fun decode(entity: Entity, context: CodingContext): LazyDecodableDataSet<Any> {
        val string = entity.data.toString(Charset.defaultCharset())
        val prototype = Json.decodeFromString(serializer<DataSetPrototype>(), string)
        return SimpleDataSet(prototype)
    }

    override fun encode(value: LazyDecodableDataSet<Any>, context: CodingContext): Entity {
        val prototype = value.toPrototype()
        val string = Json.encodeToString(serializer(), prototype)
        return Entity(RawData.copyFrom(string, Charset.defaultCharset()))
    }
}
