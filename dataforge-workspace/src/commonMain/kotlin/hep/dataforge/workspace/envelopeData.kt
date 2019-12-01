package hep.dataforge.workspace

import hep.dataforge.data.Data
import hep.dataforge.data.await
import hep.dataforge.io.Envelope
import hep.dataforge.io.IOFormat
import hep.dataforge.io.SimpleEnvelope
import hep.dataforge.io.readWith
import kotlinx.coroutines.coroutineScope
import kotlinx.io.ArrayBinary
import kotlin.reflect.KClass

/**
 * Convert an [Envelope] to a data via given format. The actual parsing is done lazily.
 */
fun <T : Any> Envelope.toData(type: KClass<out T>, format: IOFormat<T>): Data<T> = Data(type, meta) {
    data?.readWith(format) ?: error("Can't convert envelope without data to Data")
}

suspend fun <T : Any> Data<T>.toEnvelope(format: IOFormat<T>): Envelope {
    val obj = coroutineScope {
        await(this)
    }
    val binary = ArrayBinary.write {
        format.run { writeObject(obj) }
    }
    return SimpleEnvelope(meta, binary)
}