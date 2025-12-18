package space.kscience.dataforge.io

import kotlinx.io.readByteArray
import kotlinx.io.writeString
import kotlin.test.Test
import kotlin.test.assertEquals


class EnvelopeFormatTest {
    val envelope = Envelope {
        type = "test.format"
        meta {
            "d" put 22.2
        }
        data {
            writeString("12345678")
        }
    }

    @Test
    fun testTaggedFormat() = with(TaggedEnvelopeFormat) {
        val byteArray = writeToByteArray(envelope)
        val res = readFromByteArray(byteArray)
        assertEquals(envelope.meta, res.meta)
        val bytes = res.data?.read {
            readByteArray()
        }
//        println(byteArray.decodeToString())
        assertEquals("12345678", bytes?.decodeToString())
    }

    @Test
    fun testTaglessFormat() = with(TaglessEnvelopeFormat) {
        val byteArray = writeToByteArray(envelope)
        println(byteArray.decodeToString())
        val res = readFromByteArray(byteArray)
        assertEquals(envelope.meta, res.meta)
        val bytes = res.data?.read {
            readByteArray()
        }
        assertEquals("12345678", bytes?.decodeToString())
    }

    @Test
    fun testManualDftl() {
        val envelopeString = """
            #~DFTL
            #~META
            {
                "@envelope": {
                    "type": "test.format"
                },
                "d": 22.2
            }
            #~DATA
            12345678
        """.trimIndent()
        val res = TaglessEnvelopeFormat.readFromByteArray(envelopeString.encodeToByteArray())
        assertEquals(envelope.meta, res.meta)
        val bytes = res.data?.read {
            readByteArray()
        }
        assertEquals("12345678", bytes?.decodeToString())
    }
}