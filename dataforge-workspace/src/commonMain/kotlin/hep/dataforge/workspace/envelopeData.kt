package hep.dataforge.workspace

import hep.dataforge.data.Data
import hep.dataforge.data.await
import hep.dataforge.io.Envelope
import hep.dataforge.io.IOFormat
import hep.dataforge.io.SimpleEnvelope
import hep.dataforge.io.readWith
import kotlinx.coroutines.coroutineScope
import kotlinx.io.Input
import kotlinx.io.buildPacket
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
    val binary = object : Binary {
        override fun <R> read(block: Input.() -> R): R {
            //TODO optimize away additional copy by creating inputs that reads directly from output
            val packet = buildPacket {
                format.run { writeObject(obj) }
            }
            return packet.block()
        }

    }
    return SimpleEnvelope(meta, binary)
}