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
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

/**
 * And interface for serialization facilities
 */

interface IOFormat<T : Any> {
    fun Output.writeThis(obj: T)
    fun Input.readThis(): T


}

fun <T : Any> IOFormat<T>.writePacket(obj: T): ByteReadPacket = buildPacket { writeThis(obj) }
fun <T : Any> IOFormat<T>.writeBytes(obj: T): ByteArray = buildPacket { writeThis(obj) }.readBytes()
fun <T : Any> IOFormat<T>.readBytes(array: ByteArray): T = ByteReadPacket(array).readThis()

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