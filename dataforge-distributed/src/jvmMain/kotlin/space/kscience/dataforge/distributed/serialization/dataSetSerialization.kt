package space.kscience.dataforge.distributed.serialization

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import space.kscience.dataforge.data.Data
import space.kscience.dataforge.data.DataSet
import space.kscience.dataforge.data.NamedData
import space.kscience.dataforge.data.asIterable
import space.kscience.dataforge.data.component1
import space.kscience.dataforge.data.component2
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.names.Name
import kotlin.reflect.KType

/**
 * [DataSet] representation that is trivially serializable.
 */
@Serializable
internal data class DataSetPrototype(val meta: Meta, val data: Map<String, DataPrototype>) {
    fun <T : Any> toDataSet(type: KType, serializer: KSerializer<T>): DataSet<T> {
        val data = data
            .mapKeys { (name, _) -> Name.of(name) }
            .mapValues { (_, dataPrototype) -> dataPrototype.toData(type, serializer) }
        return SerializableDataSetImpl(type, data, meta)
    }

    companion object {
        suspend fun <T : Any> of(dataSet: DataSet<T>, serializer: KSerializer<T>): DataSetPrototype = coroutineScope {
            val prototypes = dataSet.asIterable().map { (name, data) ->
                name.toString() to async { DataPrototype.of(data, serializer) }
            }
            val map = prototypes.associate { (name, deferred) -> name to deferred.await() }
            DataSetPrototype(dataSet.meta, map)
        }
    }
}

/**
 * Trivial [DataSet] implementation.
 */
private class SerializableDataSetImpl<T : Any>(
    override val dataType: KType,
    private val data: Map<Name, Data<T>>,
    override val meta: Meta,
) : DataSet<T> {

    /**
     * Trivial named data implementation.
     */
    private class SimpleNamedData<T : Any>(override val name: Name, override val data: Data<T>) :
        NamedData<T>, Data<T> by data

    override fun iterator(): Iterator<NamedData<T>> =
        data
            .asSequence()
            .map { (name, data) -> SimpleNamedData(name, data) }
            .iterator()

    override fun get(name: Name): Data<T>? = data[name]
}
