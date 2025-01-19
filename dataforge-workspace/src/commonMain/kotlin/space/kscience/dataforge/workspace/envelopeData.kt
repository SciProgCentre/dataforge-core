package space.kscience.dataforge.workspace

import space.kscience.dataforge.data.Data
import space.kscience.dataforge.data.await
import space.kscience.dataforge.io.*
import space.kscience.dataforge.misc.UnsafeKType
import kotlin.reflect.typeOf


/**
 * Convert an [Envelope] to a data via given format. The actual parsing is done lazily.
 */
@OptIn(UnsafeKType::class)
public inline fun <reified T : Any> Envelope.toData(format: IOReader<T>): Data<T> = Data(typeOf<T>(), meta) {
    data?.readWith(format) ?: error("Can't convert envelope without data to Data")
}

public suspend fun <T : Any> Data<T>.toEnvelope(format: IOWriter<T>): Envelope {
    val obj = await()
    val binary = Binary(obj, format)
    return Envelope(meta, binary)
}