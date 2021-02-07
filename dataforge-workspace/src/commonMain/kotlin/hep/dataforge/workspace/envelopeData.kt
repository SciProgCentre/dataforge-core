package hep.dataforge.workspace

import hep.dataforge.data.Data
import hep.dataforge.data.await
import hep.dataforge.io.*
import hep.dataforge.misc.DFInternal

/**
 * Convert an [Envelope] to a data via given format. The actual parsing is done lazily.
 */
@OptIn(DFInternal::class)
public fun <T : Any> Envelope.toData(format: IOFormat<T>): Data<T> {
    return Data(format.type, meta) {
        data?.readWith(format) ?: error("Can't convert envelope without data to Data")
    }
}

public suspend fun <T : Any> Data<T>.toEnvelope(format: IOFormat<T>): Envelope {
    val obj = await()
    val binary = format.toBinary(obj)
    return SimpleEnvelope(meta, binary)
}