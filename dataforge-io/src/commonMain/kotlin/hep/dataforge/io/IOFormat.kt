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
import kotlin.reflect.KClass

/**
 * And interface for reading and writing objects into with IO streams
 */
interface IOFormat<T : Any> {
    fun Output.writeObject(obj: T)
    fun Input.readObject(): T
}

fun <T : Any> Input.readWith(format: IOFormat<T>): T = format.run { readObject() }
fun <T : Any> Output.writeWith(format: IOFormat<T>, obj: T) = format.run { writeObject(obj) }

class ListIOFormat<T : Any>(val format: IOFormat<T>) : IOFormat<List<T>> {
    override fun Output.writeObject(obj: List<T>) {
        writeInt(obj.size)
        format.run {
            obj.forEach {
                writeObject(it)
            }
        }
    }

    override fun Input.readObject(): List<T> {
        val size = readInt()
        return format.run {
            List(size) { readObject() }
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

fun <T : Any> IOFormat<T>.writePacket(obj: T): ByteReadPacket = buildPacket { writeObject(obj) }

@Deprecated("Not to be used outside tests due to double buffer write")
fun <T : Any> IOFormat<T>.writeBytes(obj: T): ByteArray = buildPacket { writeObject(obj) }.readBytes()
@Deprecated("Not to be used outside tests due to double buffer write")
fun <T : Any> IOFormat<T>.readBytes(array: ByteArray): T = buildPacket { writeFully(array) }.readObject()

object DoubleIOFormat : IOFormat<Double>, IOFormatFactory<Double> {
    override fun invoke(meta: Meta, context: Context): IOFormat<Double> = this

    override val name: Name = "double".asName()

    override val type: KClass<out Double> get() = Double::class

    override fun Output.writeObject(obj: Double) {
        writeDouble(obj)
    }

    override fun Input.readObject(): Double = readDouble()
}

object ValueIOFormat : IOFormat<Value>, IOFormatFactory<Value> {
    override fun invoke(meta: Meta, context: Context): IOFormat<Value> = this

    override val name: Name = "value".asName()

    override val type: KClass<out Value> get() = Value::class

    override fun Output.writeObject(obj: Value) {
        BinaryMetaFormat.run { writeValue(obj) }
    }

    override fun Input.readObject(): Value {
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


    override fun Output.writeObject(obj: T) {
        val bytes = Cbor.plain.dump(serializer, obj)
        writeFully(bytes)
    }

    override fun Input.readObject(): T {
        //FIXME reads the whole input
        val bytes = readBytes()
        return Cbor.plain.load(serializer, bytes)
    }
}