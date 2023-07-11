package space.kscience.dataforge.io

import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.readByteArray
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.Factory
import space.kscience.dataforge.io.IOFormatFactory.Companion.IO_FORMAT_TYPE
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.misc.Named
import space.kscience.dataforge.misc.Type
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.asName
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Reader of a custom object from input
 */
public interface IOReader<out T> {
    /**
     * The type of object being read
     */
    public val type: KType

    public fun readObject(source: Source): T

    public fun readObject(binary: Binary): T = binary.read { readObject(this) }

    public companion object {
        /**
         * no-op reader for binaries.
         */
        public val binary: IOReader<Binary> = object : IOReader<Binary> {
            override val type: KType = typeOf<Binary>()

            override fun readObject(source: Source): Binary = source.readByteArray().asBinary()

            override fun readObject(binary: Binary): Binary = binary
        }
    }
}

public inline fun <reified T> IOReader(crossinline read: Source.() -> T): IOReader<T> = object : IOReader<T> {
    override val type: KType = typeOf<T>()

    override fun readObject(source: Source): T = source.read()
}

public fun interface IOWriter<in T> {
    public fun writeObject(sink: Sink, obj: T)
}

/**
 * And interface for reading and writing objects into with IO streams
 */
public interface IOFormat<T> : IOReader<T>, IOWriter<T>

public fun <T : Any> Source.readObject(format: IOReader<T>): T = format.readObject(this@readObject)

public fun <T : Any> IOFormat<T>.readObjectFrom(binary: Binary): T = binary.read {
    readObject(this)
}

/**
 * Read given binary as an object using given format
 */
public fun <T : Any> Binary.readWith(format: IOReader<T>): T = read {
    readObject(format)
}

public fun <T : Any> Sink.writeObject(format: IOWriter<T>, obj: T): Unit =
    format.writeObject(this@writeObject, obj)


@Type(IO_FORMAT_TYPE)
public interface IOFormatFactory<T : Any> : Factory<IOFormat<T>>, Named {
    /**
     * Explicit type for dynamic type checks
     */
    public val type: KType

    public companion object {
        public const val IO_FORMAT_TYPE: String = "io.format"
        public val NAME_KEY: Name = "name".asName()
        public val META_KEY: Name = "meta".asName()
    }
}

public fun <T : Any> Binary(obj: T, format: IOWriter<T>): Binary = Binary { format.writeObject(this, obj) }

public object DoubleIOFormat : IOFormat<Double>, IOFormatFactory<Double> {
    override fun build(context: Context, meta: Meta): IOFormat<Double> = this

    override val name: Name = "double".asName()

    override val type: KType get() = typeOf<Double>()

    override fun writeObject(sink: Sink, obj: Double) {
        sink.writeLong(obj.toBits())
    }

    override fun readObject(source: Source): Double = Double.fromBits(source.readLong())
}