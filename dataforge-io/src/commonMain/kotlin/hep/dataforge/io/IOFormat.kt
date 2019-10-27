package hep.dataforge.io

import hep.dataforge.context.Context
import hep.dataforge.context.Factory
import hep.dataforge.context.Named
import hep.dataforge.io.IOFormatFactory.Companion.IO_FORMAT_TYPE
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaItem
import hep.dataforge.names.Name
import hep.dataforge.names.asName
import hep.dataforge.provider.Type
import hep.dataforge.values.Value
import kotlinx.io.core.*
import kotlinx.io.pool.ObjectPool
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.serializer
import kotlin.math.min
import kotlin.reflect.KClass

/**
 * And interface for reading and writing objects into with IO streams
 */
interface IOFormat<T : Any> {
    fun Output.writeThis(obj: T)
    fun Input.readThis(): T
}

fun <T : Any> Input.readWith(format: IOFormat<T>): T = format.run { readThis() }
fun <T : Any> Output.writeWith(format: IOFormat<T>, obj: T) = format.run { writeThis(obj) }

class ListIOFormat<T : Any>(val format: IOFormat<T>) : IOFormat<List<T>> {
    override fun Output.writeThis(obj: List<T>) {
        writeInt(obj.size)
        format.run {
            obj.forEach {
                writeThis(it)
            }
        }
    }

    override fun Input.readThis(): List<T> {
        val size = readInt()
        return format.run {
            List(size) { readThis() }
        }
    }
}

val <T : Any> IOFormat<T>.list get() = ListIOFormat(this)

fun ObjectPool<IoBuffer>.fill(block: IoBuffer.() -> Unit): IoBuffer {
    val buffer = borrow()
    return try {
        buffer.apply(block)
    } catch (ex: Exception) {
        //recycle(buffer)
        throw ex
    }
}

@Type(IO_FORMAT_TYPE)
interface IOFormatFactory<T : Any> : Factory<IOFormat<T>>, Named {
    /**
     * Explicit type for dynamic type checks
     */
    val type: KClass<out T>

    companion object {
        const val IO_FORMAT_TYPE = "io.format"
    }
}

@Deprecated("To be removed in io-2")
inline fun buildPacketWithoutPool(headerSizeHint: Int = 0, block: BytePacketBuilder.() -> Unit): ByteReadPacket {
    val builder = BytePacketBuilder(headerSizeHint, IoBuffer.NoPool)
    block(builder)
    return builder.build()
}

//@Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_OVERRIDE")
//internal fun <R> Input.useAtMost(most: Int, reader: Input.() -> R): R {
//    val limitedInput: Input = object : AbstractInput(
//        IoBuffer.Pool.borrow(),
//        remaining = most.toLong(),
//        pool = IoBuffer.Pool
//    ) {
//        var read = 0
//        override fun closeSource() {
//            this@useAtMost.close()
//        }
//
//        override fun fill(): IoBuffer? {
//            if (read >= most) return null
//            return IoBuffer.Pool.fill {
//                reserveEndGap(IoBuffer.ReservedSize)
//                read += this@useAtMost.peekTo(this, max = most - read)
//            }
//        }
//
//    }
//    return limitedInput.reader()
//}

fun <T : Any> IOFormat<T>.writePacket(obj: T): ByteReadPacket = buildPacket { writeThis(obj) }
fun <T : Any> IOFormat<T>.writeBytes(obj: T): ByteArray = buildPacket { writeThis(obj) }.readBytes()
fun <T : Any> IOFormat<T>.readBytes(array: ByteArray): T {
    //= ByteReadPacket(array).readThis()
    val byteArrayInput: Input = object : AbstractInput(
        IoBuffer.Pool.borrow(),
        remaining = array.size.toLong(),
        pool = IoBuffer.Pool
    ) {
        var written = 0
        override fun closeSource() {
            // do nothing
        }

        override fun fill(): IoBuffer? {
            if (array.size - written <= 0) return null

            return IoBuffer.Pool.fill {
                reserveEndGap(IoBuffer.ReservedSize)
                val toWrite = min(capacity, array.size - written)
                writeFully(array, written, toWrite)
                written += toWrite
            }
        }

    }
    return byteArrayInput.readThis()
}

object DoubleIOFormat : IOFormat<Double>, IOFormatFactory<Double> {
    override fun invoke(meta: Meta, context: Context): IOFormat<Double> = this

    override val name: Name = "double".asName()

    override val type: KClass<out Double> get() = Double::class

    override fun Output.writeThis(obj: Double) {
        writeDouble(obj)
    }

    override fun Input.readThis(): Double = readDouble()
}

object ValueIOFormat : IOFormat<Value>, IOFormatFactory<Value> {
    override fun invoke(meta: Meta, context: Context): IOFormat<Value> = this

    override val name: Name = "value".asName()

    override val type: KClass<out Value> get() = Value::class

    override fun Output.writeThis(obj: Value) {
        BinaryMetaFormat.run { writeValue(obj) }
    }

    override fun Input.readThis(): Value {
        return (BinaryMetaFormat.run { readMetaItem() } as? MetaItem.ValueItem)?.value
            ?: error("The item is not a value")
    }
}

/**
 * Experimental
 */
@ImplicitReflectionSerializer
class SerializerIOFormat<T : Any>(
    type: KClass<T>,
    val serializer: KSerializer<T> = type.serializer()
) : IOFormat<T> {

    //override val name: Name = type.simpleName?.toName() ?: EmptyName


    override fun Output.writeThis(obj: T) {
        val bytes = Cbor.plain.dump(serializer, obj)
        writeFully(bytes)
    }

    override fun Input.readThis(): T {
        //FIXME reads the whole input
        val bytes = readBytes()
        return Cbor.plain.load(serializer, bytes)
    }
}