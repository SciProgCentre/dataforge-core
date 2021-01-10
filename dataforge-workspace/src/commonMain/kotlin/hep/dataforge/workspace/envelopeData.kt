package hep.dataforge.workspace

import hep.dataforge.data.Data
import hep.dataforge.data.await
import hep.dataforge.io.*
import kotlin.reflect.KClass

/**
 * Convert an [Envelope] to a data via given format. The actual parsing is done lazily.
 */
public fun <T : Any> Envelope.toData(format: IOFormat<T>): Data<T> {
    @Suppress("UNCHECKED_CAST")
    val kclass: KClass<T> = format.type.classifier as? KClass<T> ?: error("IOFormat type is not a class")
    return Data(kclass, meta) {
        data?.readWith(format) ?: error("Can't convert envelope without data to Data")
    }
}

public suspend fun <T : Any> Data<T>.toEnvelope(format: IOFormat<T>): Envelope {
    val obj = await()
    val binary = format.toBinary(obj)
    return SimpleEnvelope(meta, binary)
}