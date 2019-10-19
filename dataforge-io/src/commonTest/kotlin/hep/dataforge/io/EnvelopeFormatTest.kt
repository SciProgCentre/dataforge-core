package hep.dataforge.io

import kotlin.test.Test
import kotlin.test.assertEquals


class EnvelopeFormatTest {
    val envelope = Envelope.invoke {
        type = "test.format"
        meta{
            "d" to 22.2
        }
        data{
            writeDouble(22.2)
        }
    }

    @ExperimentalStdlibApi
    @Test
    fun testTaggedFormat(){
        TaggedEnvelopeFormat().run {
            val bytes = writeBytes(envelope)
            println(bytes.decodeToString())
            val res = readBytes(bytes)
            assertEquals(envelope.meta,res.meta)
            val double = res.data?.read {
                readDouble()
            }
            assertEquals(22.2, double)
        }
    }
}