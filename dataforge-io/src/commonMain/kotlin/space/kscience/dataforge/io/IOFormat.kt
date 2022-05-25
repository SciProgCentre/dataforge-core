package space.kscience.dataforge.io

import io.ktor.utils.io.core.*
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.Factory
import space.kscience.dataforge.io.IOFormat.Companion.NAME_KEY
import space.kscience.dataforge.io.IOFormatFactory.Companion.IO_FORMAT_TYPE
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.MetaRepr
import space.kscience.dataforge.misc.Named
import space.kscience.dataforge.misc.Type
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.asName
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * And interface for reading and writing objects into with IO streams
 */
public interface IOFormat<T : Any> : MetaRepr {
    public val type: KType

    public fun writeObject(output: Output, obj: T)
    public fun readObject(input: Input): T

    public companion object {
        public val NAME_KEY: Name = "name".asName()
        public val META_KEY: Name = "meta".asName()
    }
}

public fun <T : Any> Input.readWith(format: IOFormat<T>): T = format.readObject(this@readWith)

public fun <T: Any> IOFormat<T>.readObject(binary: Binary): T = binary.read {
    readObject(this)
}

/**
 * Read given binary as object using given format
 */
public fun <T : Any> Binary.readWith(format: IOFormat<T>): T = read {
    readWith(format)
}

public fun <T : Any> Output.writeWith(format: IOFormat<T>, obj: T): Unit =
    format.run { writeObject(this@writeWith, obj) }

public inline fun <reified T : Any> IOFormat.Companion.listOf(
    format: IOFormat<T>,
): IOFormat<List<T>> = object : IOFormat<List<T>> {
    override val type: KType = typeOf<List<T>>()

    override fun writeObject(output: Output, obj: List<T>) {
        output.writeInt(obj.size)
        format.run {
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

//public fun ObjectPool<Buffer>.fill(block: Buffer.() -> Unit): Buffer {
//    val buffer = borrow()
//    return try {
//        buffer.apply(block)
//    } catch (ex: Exception) {
//        //recycle(buffer)
//        throw ex
//    }
//}

@Type(IO_FORMAT_TYPE)
public interface IOFormatFactory<T : Any> : Factory<IOFormat<T>>, Named, MetaRepr {
    /**
     * Explicit type for dynamic type checks
     */
    public val type: KType

    override fun toMeta(): Meta = Meta {
        NAME_KEY put name.toString()
    }

    public companion object {
        public const val IO_FORMAT_TYPE: String = "io.format"
    }
}

public fun <T : Any> IOFormat<T>.toBinary(obj: T): Binary = Binary { writeObject(this, obj) }

public object DoubleIOFormat : IOFormat<Double>, IOFormatFactory<Double> {
    override fun build(context: Context, meta: Meta): IOFormat<Double> = this

    override val name: Name = "double".asName()

    override val type: KType get() = typeOf<Double>()

    override fun writeObject(output: Output, obj: kotlin.Double) {
        output.writeDouble(obj)
    }

    override fun readObject(input: Input): Double = input.readDouble()
}

//public object ValueIOFormat : IOFormat<Value>, IOFormatFactory<Value> {
//    override fun invoke(meta: Meta, context: Context): IOFormat<Value> = this
//
//    override val name: Name = "value".asName()
//
//    override val type: KType get() = typeOf<Value>()
//
//    override fun writeObject(output: Output, obj: Value) {
//        BinaryMetaFormat.run { output.writeValue(obj) }
//    }
//
//    override fun readObject(input: Input): Value {
//        return (BinaryMetaFormat.run { input.readMetaItem() } as? MetaItemValue)?.value
//            ?: error("The item is not a value")
//    }
//}