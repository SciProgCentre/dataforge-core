package space.kscience.dataforge.distributed.serialization

import space.kscience.dataforge.data.DataSet
import kotlin.reflect.KType

/**
 * Represents [DataSet] that should be initialized before usage.
 */
internal interface SerializableDataSet<T : Any> : DataSet<T> {
    fun finishDecoding(type: KType)
}

internal class SerializableDataSetAdapter<T : Any>(dataSet: DataSet<T>) :
    SerializableDataSet<T>, DataSet<T> by dataSet {
    override fun finishDecoding(type: KType) = Unit
}
