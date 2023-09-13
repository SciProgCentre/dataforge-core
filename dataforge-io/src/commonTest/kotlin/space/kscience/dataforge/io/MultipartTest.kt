package space.kscience.dataforge.io

import kotlinx.io.writeString
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
                writeString("Hello World $it")
                repeat(300) {
                    writeString("$it ")
                }
            }
        }
    }

    val partsEnvelope = Envelope {
        envelopes(envelopes)
    }

    @Test
    fun testParts() {
        val format = TaggedEnvelopeFormat
        val singleEnvelopeData = Binary(envelopes[0], format)
        val singleEnvelopeSize = singleEnvelopeData.size
        val bytes = Binary(partsEnvelope, format)
        assertTrue(envelopes.size * singleEnvelopeSize < bytes.size)
        val reconstructed = bytes.readWith(format)
        println(reconstructed.meta)
        val parts = reconstructed.parts()
        val envelope = parts[2].envelope()
        assertEquals(2, envelope.meta["value"].int)
        println(reconstructed.data!!.size)
    }

}