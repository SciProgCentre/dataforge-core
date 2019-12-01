package hep.dataforge.io

import hep.dataforge.meta.DFExperimental
import hep.dataforge.meta.get
import hep.dataforge.meta.int
import kotlinx.io.text.writeUtf8String

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DFExperimental
class MultipartTest {
    val envelopes = (0..5).map {
        Envelope {
            meta {
                "value" put it
            }
            data {
                writeUtf8String("Hello World $it")
//                repeat(2000) {
//                    writeInt(it)
//                }
            }
        }
    }

    val partsEnvelope = Envelope {
        multipart(envelopes, TaggedEnvelopeFormat)
    }

    @Test
    fun testParts() {
        TaggedEnvelopeFormat.run {
            val singleEnvelopeData = writeBytes(envelopes[0])
            val singleEnvelopeSize = singleEnvelopeData.size
            val bytes = writeBytes(partsEnvelope)
            assertTrue(5*singleEnvelopeSize < bytes.size)
            val reconstructed = bytes.readWith(this)
            val parts = reconstructed.parts()?.toList() ?: emptyList()
            assertEquals(2, parts[2].meta["value"].int)
            println(reconstructed.data!!.size)
        }
    }

}