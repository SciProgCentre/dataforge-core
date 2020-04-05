package hep.dataforge.io

import hep.dataforge.context.Context
import hep.dataforge.context.Factory
import hep.dataforge.context.Named
import hep.dataforge.io.IOFormatFactory.Companion.IO_FORMAT_TYPE
import hep.dataforge.io.IOPlugin.Companion.IO_FORMAT_NAME_KEY
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaItem
import hep.dataforge.meta.MetaRepr
import hep.dataforge.names.Name
import hep.dataforge.names.asName
import hep.dataforge.provider.Type
import hep.dataforge.values.Value
import kotlinx.io.*
import kotlinx.io.buffer.Buffer
import kotlinx.io.pool.ObjectPool
import kotlin.reflect.KClass

/**
 * And interface for reading and writing objects into with IO streams
 */
interface IOFormat<T : Any> : MetaRepr {
    fun Output.writeObject(obj: T)
    fun Input.readObject(): T
}

fun <T : Any> Input.readWith(format: IOFormat<T>): T = format.run { readObject() }

/**
 * Read given binary as object using given format
 */
fun <T : Any> Binary.readWith(format: IOFormat<T>): T = read {
    readWith(format)
}

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

    override fun toMeta(): Meta = Meta {
        IO_FORMAT_NAME_KEY put "list"
        "contentFormat" put format.toMeta()
    }
}

val <T : Any> IOFormat<T>.list get() = ListIOFormat(this)

fun ObjectPool<Buffer>.fill(block: Buffer.() -> Unit): Buffer {
    val buffer = borrow()
    return try {
        buffer.apply(block)
    } catch (ex: Exception) {
        //recycle(buffer)
        throw ex
    }
}

@Type(IO_FORMAT_TYPE)
interface IOFormatFactory<T : Any> : Factory<IOFormat<T>>, Named, MetaRepr {
    /**
     * Explicit type for dynamic type checks
     */
    val type: KClass<out T>

    override fun toMeta(): Meta = Meta {
        IO_FORMAT_NAME_KEY put name.toString()
    }

    companion object {
        const val IO_FORMAT_TYPE = "io.format"
    }
}

fun <T : Any> IOFormat<T>.toBinary(obj: T): Binary = Binary { writeObject(obj) }

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