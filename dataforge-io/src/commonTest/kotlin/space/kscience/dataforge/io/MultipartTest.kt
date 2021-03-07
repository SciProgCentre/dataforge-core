package space.kscience.dataforge.io

import space.kscience.dataforge.context.Global
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.int
import space.kscience.dataforge.misc.DFExperimental
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DFExperimental
class MultipartTest {
    val io: IOPlugin = Global.io

    val envelopes = (0 until 5).map {
        Envelope {
            meta {
                "value" put it
            }
            data {
                writeUtf8String("Hello World $it")
                repeat(300) {
                    writeRawString("$it ")
                }
            }
        }
    }

    val partsEnvelope = Envelope {
        envelopes(envelopes, TaglessEnvelopeFormat)
    }

    @Test
    fun testParts() {
        TaglessEnvelopeFormat.run {
            val singleEnvelopeData = toBinary(envelopes[0])
            val singleEnvelopeSize = singleEnvelopeData.size
            val bytes = toBinary(partsEnvelope)
            assertTrue(envelopes.size * singleEnvelopeSize < bytes.size)
            val reconstructed = bytes.readWith(this)
            println(reconstructed.meta)
            val parts = reconstructed.parts()
            val envelope = parts[2].envelope(io)
            assertEquals(2, envelope.meta["value"].int)
            println(reconstructed.data!!.size)
        }
    }

}