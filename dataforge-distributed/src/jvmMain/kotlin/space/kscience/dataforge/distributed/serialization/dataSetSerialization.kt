package space.kscience.dataforge.distributed.serialization

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
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
    fun <T : Any> toDataSet(type: KType, serializer: KSerializer<T>): DataSet<T> {
        val data = data
            .mapKeys { (name, _) -> Name.of(name) }
            .mapValues { (_, dataPrototype) -> dataPrototype.toData(type, serializer) }
        return SerializableDataSetImpl(type, data)
    }

    companion object {
        suspend fun <T : Any> of(dataSet: DataSet<T>, serializer: KSerializer<T>): DataSetPrototype = coroutineScope {
            val flow = mutableListOf<Pair<String, Deferred<DataPrototype>>>()
            dataSet.flowData().map { (name, data) ->
                name.toString() to async { DataPrototype.of(data, serializer) }
            }.toList(flow)
            DataSetPrototype(flow.associate { (name, deferred) -> name to deferred.await() })
        }
    }
}

/**
 * Trivial [DataSet] implementation.
 */
private class SerializableDataSetImpl<T : Any>(
    override val dataType: KType,
    private val data: Map<Name, Data<T>>,
) : DataSet<T> {

    override fun flowData(): Flow<NamedData<T>> =
        data.map { (name, data) -> SimpleNamedData(name, data) }.asFlow()

    override suspend fun getData(name: Name): Data<T>? = data[name]

    /**
     * Trivial named data implementation.
     */
    private class SimpleNamedData<T : Any>(override val name: Name, override val data: Data<T>) :
        NamedData<T>, Data<T> by data
}
