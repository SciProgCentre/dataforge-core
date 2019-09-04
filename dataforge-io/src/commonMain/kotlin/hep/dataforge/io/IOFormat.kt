package hep.dataforge.io

import hep.dataforge.io.IOFormat.Companion.TYPE
import hep.dataforge.provider.Type
import kotlinx.io.core.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.cbor.Cbor

/**
 * And interface for serialization facilities
 */
@Type(TYPE)
interface IOFormat<T : Any> {
    fun Output.writeThis(obj: T)
    fun Input.readThis(): T

    companion object {
        const val TYPE = "ioFormat"
    }
}

fun <T : Any> IOFormat<T>.writePacket(obj: T): ByteReadPacket = buildPacket { writeThis(obj) }
fun <T : Any> IOFormat<T>.writeBytes(obj: T): ByteArray = buildPacket { writeThis(obj) }.readBytes()


object DoubleIOFormat: IOFormat<Double>{
    override fun Output.writeThis(obj: Double) {
        writeDouble(obj)
    }
    override fun Input.readThis(): Double = readDouble()
}

/**
 * Experimental
 */
class SerializerIOFormat<T : Any>(val serializer: KSerializer<T>) : IOFormat<T> {
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