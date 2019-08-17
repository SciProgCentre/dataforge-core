package hep.dataforge.workspace

import hep.dataforge.data.Data
import hep.dataforge.io.Envelope
import hep.dataforge.io.IOFormat
import hep.dataforge.io.readWith
import kotlin.reflect.KClass

/**
 * Convert an [Envelope] to a data via given format. The actual parsing is done lazily.
 */
fun <T : Any> Envelope.toData(type: KClass<out T>, format: IOFormat<T>): Data<T> = Data(type, meta) {
    data?.readWith(format) ?: error("Can't convert envelope without data to Data")
}