package hep.dataforge.io

import kotlinx.io.readDouble
import kotlinx.io.writeDouble
import kotlin.test.Test
import kotlin.test.assertEquals


class EnvelopeFormatTest {
    val envelope = Envelope.invoke {
        type = "test.format"
        meta{
            "d" put 22.2
        }
        data{
            writeDouble(22.2)
//            repeat(2000){
//                writeInt(it)
//            }
        }
    }

    @Test
    fun testTaggedFormat(){
        TaggedEnvelopeFormat.run {
            val byteArray = this.writeByteArray(envelope)
            //println(byteArray.decodeToString())
            val res = readByteArray(byteArray)
            assertEquals(envelope.meta,res.meta)
            val double = res.data?.read {
                readDouble()
            }
            assertEquals(22.2, double)
        }
    }

    @Test
    fun testTaglessFormat(){
        TaglessEnvelopeFormat.run {
            val byteArray = writeByteArray(envelope)
            //println(byteArray.decodeToString())
            val res = readByteArray(byteArray)
            assertEquals(envelope.meta,res.meta)
            val double = res.data?.read {
                readDouble()
            }
            assertEquals(22.2, double)
        }
    }
}