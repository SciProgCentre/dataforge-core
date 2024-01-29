package space.kscience.dataforge.io

import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.readByteArray
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.Factory
import space.kscience.dataforge.io.IOFormatFactory.Companion.IO_FORMAT_TYPE
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.misc.DfType
import space.kscience.dataforge.misc.Named
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.asName
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Reader of a custom object from input
 */
public fun interface IOReader<out T> {

    public fun readFrom(source: Source): T

    public fun readFrom(binary: Binary): T = binary.read { readFrom(this) }

    public companion object {
        /**
         * no-op reader for binaries.
         */
        public val binary: IOReader<Binary> = object : IOReader<Binary> {

            override fun readFrom(source: Source): Binary = source.readByteArray().asBinary()

            override fun readFrom(binary: Binary): Binary = binary
        }
    }
}

public inline fun <reified T> IOReader(crossinline read: Source.() -> T): IOReader<T> = object : IOReader<T> {
    override fun readFrom(source: Source): T = source.read()
}

public fun interface IOWriter<in T> {
    public fun writeTo(sink: Sink, obj: T)
}

/**
 * And interface for reading and writing objects into with IO streams
 */
public interface IOFormat<T> : IOReader<T>, IOWriter<T>

public fun <T> Source.readWith(format: IOReader<T>): T = format.readFrom(this)

/**
 * Read given binary as an object using given format
 */
public fun <T> Binary.readWith(format: IOReader<T>): T = read {
    this.readWith(format)
}

/**
 * Write an object to the [Sink] with given [format]
 */
public fun <T> Sink.writeWith(format: IOWriter<T>, obj: T): Unit =
    format.writeTo(this, obj)


@DfType(IO_FORMAT_TYPE)
public interface IOFormatFactory<T> : Factory<IOFormat<T>>, Named {
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

public fun <T> Binary(obj: T, format: IOWriter<T>): Binary = Binary { format.writeTo(this, obj) }

public object FloatIOFormat : IOFormat<Float>, IOFormatFactory<Float> {
    override fun build(context: Context, meta: Meta): IOFormat<Float> = this

    override val name: Name = "float32".asName()

    override val type: KType get() = typeOf<Float>()

    override fun writeTo(sink: Sink, obj: Float) {
        sink.writeFloat(obj)
    }

    override fun readFrom(source: Source): Float = source.readFloat()
}


public object DoubleIOFormat : IOFormat<Double>, IOFormatFactory<Double> {
    override fun build(context: Context, meta: Meta): IOFormat<Double> = this

    override val name: Name = "float64".asName()

    override val type: KType get() = typeOf<Double>()

    override fun writeTo(sink: Sink, obj: Double) {
        sink.writeLong(obj.toBits())
    }

    override fun readFrom(source: Source): Double = source.readDouble()
}