package space.kscience.dataforge.workspace

import space.kscience.dataforge.data.Data
import space.kscience.dataforge.data.await
import space.kscience.dataforge.io.*
import space.kscience.dataforge.misc.DFInternal
import kotlin.reflect.KType
import kotlin.reflect.typeOf


@DFInternal
public fun <T : Any> Envelope.toData(type: KType, format: IOReader<T>): Data<T> = Data(type, meta) {
    data?.readWith(format) ?: error("Can't convert envelope without data to Data")
}

/**
 * Convert an [Envelope] to a data via given format. The actual parsing is done lazily.
 */
@OptIn(DFInternal::class)
public inline fun <reified T : Any> Envelope.toData(format: IOReader<T>): Data<T> = toData(typeOf<T>(), format)

public suspend fun <T : Any> Data<T>.toEnvelope(format: IOWriter<T>): Envelope {
    val obj = await()
    val binary = Binary(obj, format)
    return SimpleEnvelope(meta, binary)
}