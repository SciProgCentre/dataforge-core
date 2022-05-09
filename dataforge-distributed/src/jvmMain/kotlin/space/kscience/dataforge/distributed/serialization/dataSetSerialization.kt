package space.kscience.dataforge.distributed.serialization

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import space.kscience.dataforge.data.Data
import space.kscience.dataforge.data.DataSet
import space.kscience.dataforge.data.NamedData
import space.kscience.dataforge.data.component1
import space.kscience.dataforge.data.component2
import space.kscience.dataforge.names.Name
import kotlin.reflect.KType

/**
 * [DataSet] representation that is trivially serializable.
 */
@Serializable
internal data class DataSetPrototype(val data: Map<String, DataPrototype>) {
    fun toDataSet(): SerializableDataSet<Any> =
        SerializableDataSetImpl(this)

    fun toJson(): String = Json.encodeToString(serializer(), this)

    companion object {
        fun <T : Any> of(dataSet: DataSet<T>): DataSetPrototype = runBlocking {
            val serializer = serializer(dataSet.dataType)
            val map = mutableListOf<Pair<String, DataPrototype>>()
            dataSet.flowData().map { (name, data) ->
                name.toString() to DataPrototype.of(data, serializer)
            }.toList(map)
            DataSetPrototype(map.associate { it })
        }

        fun fromJson(string: String): DataSetPrototype = Json.decodeFromString(serializer(), string)
    }
}

/**
 * Trivial [SerializableDataSet] implementation.
 */
private class SerializableDataSetImpl(private val prototype: DataSetPrototype) : SerializableDataSet<Any> {

    private lateinit var type: KType
    private lateinit var data: Map<Name, Data<Any>>

    override fun finishDecoding(type: KType) {
        this.type = type
        this.data = prototype.data
            .mapKeys { (name, _) -> Name.of(name) }
            .mapValues { (_, dataPrototype) -> dataPrototype.toData(type) }
    }

    override val dataType: KType
        get() = type

    override fun flowData(): Flow<NamedData<Any>> =
        data.map { (name, data) -> SimpleNamedData(name, data) }.asFlow()

    override suspend fun getData(name: Name): Data<Any>? = data[name]

    /**
     * Trivial named data implementation.
     */
    private class SimpleNamedData(
        override val name: Name,
        override val data: Data<Any>,
    ) : NamedData<Any>, Data<Any> by data
}
