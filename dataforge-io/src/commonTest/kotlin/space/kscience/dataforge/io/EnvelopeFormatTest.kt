package space.kscience.dataforge.io

import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.readDouble
import io.ktor.utils.io.core.writeDouble
import kotlin.test.Test
import kotlin.test.assertEquals


class EnvelopeFormatTest {
    val envelope = Envelope {
        type = "test.format"
        meta {
            "d" put 22.2
        }
        data {
            writeDouble(22.2)
//            repeat(2000){
//                writeInt(it)
//            }
        }
    }

    @Test
    fun testTaggedFormat() {
        TaggedEnvelopeFormat.run {
            val byteArray = writeToByteArray(envelope)
            //println(byteArray.decodeToString())
            val res = readFromByteArray(byteArray)
            assertEquals(envelope.meta, res.meta)
            val double = res.data?.read {
                readDouble()
            }
            assertEquals(22.2, double)
        }
    }

    @Test
    fun testTaglessFormat() {
        TaglessEnvelopeFormat.run {
            val byteArray = writeToByteArray(envelope)
            //println(byteArray.decodeToString())
            val partial = readPartial(ByteReadPacket(byteArray))
            assertEquals(8, partial.dataSize?.toInt())
            val res = readFromByteArray(byteArray)
            assertEquals(envelope.meta, res.meta)
            val double = res.data?.read {
                readDouble()
            }
            assertEquals(22.2, double)
        }
    }
}