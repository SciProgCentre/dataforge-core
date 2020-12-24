package hep.dataforge.io

import hep.dataforge.context.Context
import hep.dataforge.context.Factory
import hep.dataforge.context.Named
import hep.dataforge.io.IOFormat.Companion.NAME_KEY
import hep.dataforge.io.IOFormatFactory.Companion.IO_FORMAT_TYPE
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaRepr
import hep.dataforge.meta.ValueItem
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
public interface IOFormat<T : Any> : MetaRepr {
    public fun writeObject(output: Output, obj: T)
    public fun readObject(input: Input): T

    public companion object {
        public val NAME_KEY: Name = "name".asName()
        public val META_KEY: Name = "meta".asName()
    }
}

public fun <T : Any> Input.readWith(format: IOFormat<T>): T = format.readObject(this@readWith)

/**
 * Read given binary as object using given format
 */
public fun <T : Any> Binary.readWith(format: IOFormat<T>): T = read {
    readWith(format)
}

public fun <T : Any> Output.writeWith(format: IOFormat<T>, obj: T): Unit =
    format.run { writeObject(this@writeWith, obj) }

public class ListIOFormat<T : Any>(public val format: IOFormat<T>) : IOFormat<List<T>> {
    override fun writeObject(output: Output, obj: List<T>) {
        output.writeInt(obj.size)
        this.format.run {
            obj.forEach {
                writeObject(output, it)
            }
        }
    }

    override fun readObject(input: Input): List<T> {
        val size = input.readInt()
        return format.run {
            List(size) { readObject(input) }
        }
    }

    override fun toMeta(): Meta = Meta {
        NAME_KEY put "list"
        "contentFormat" put format.toMeta()
    }
}

//public val <T : Any> IOFormat<T>.list: ListIOFormat<T> get() = ListIOFormat(this)

public fun ObjectPool<Buffer>.fill(block: Buffer.() -> Unit): Buffer {
    val buffer = borrow()
    return try {
        buffer.apply(block)
    } catch (ex: Exception) {
        //recycle(buffer)
        throw ex
    }
}

@Type(IO_FORMAT_TYPE)
public interface IOFormatFactory<T : Any> : Factory<IOFormat<T>>, Named, MetaRepr {
    /**
     * Explicit type for dynamic type checks
     */
    public val type: KClass<out T>

    override fun toMeta(): Meta = Meta {
        NAME_KEY put name.toString()
    }

    public companion object {
        public const val IO_FORMAT_TYPE: String = "io.format"
    }
}

public fun <T : Any> IOFormat<T>.toBinary(obj: T): Binary = Binary { writeObject(this, obj) }

public object DoubleIOFormat : IOFormat<Double>, IOFormatFactory<Double> {
    override fun invoke(meta: Meta, context: Context): IOFormat<Double> = this

    override val name: Name = "double".asName()

    override val type: KClass<out Double> get() = Double::class

    override fun writeObject(output: Output, obj: kotlin.Double) {
        output.writeDouble(obj)
    }

    override fun readObject(input: Input): Double = input.readDouble()
}

public object ValueIOFormat : IOFormat<Value>, IOFormatFactory<Value> {
    override fun invoke(meta: Meta, context: Context): IOFormat<Value> = this

    override val name: Name = "value".asName()

    override val type: KClass<out Value> get() = Value::class

    override fun writeObject(output: Output, obj: Value) {
        BinaryMetaFormat.run { output.writeValue(obj) }
    }

    override fun readObject(input: Input): Value {
        return (BinaryMetaFormat.run { input.readMetaItem() } as? ValueItem)?.value
            ?: error("The item is not a value")
    }
}