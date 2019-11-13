package hep.dataforge.io

import hep.dataforge.meta.get
import hep.dataforge.meta.int
import kotlinx.io.core.writeText
import kotlin.test.Test
import kotlin.test.assertEquals

class EnvelopePartsTest {
    val envelopes = (0..5).map {
        Envelope {
            meta {
                "value" put it
            }
            data {
                writeText("Hello World $it")
                repeat(200){
                    writeInt(it)
                }
            }
        }
    }
    val partsEnvelope = Envelope {
        multipart(TaggedEnvelopeFormat, envelopes)
    }

    @Test
    fun testParts() {
        val bytes = TaggedEnvelopeFormat.writeBytes(partsEnvelope)
        val reconstructed = TaggedEnvelopeFormat.readBytes(bytes)
        val parts = reconstructed.parts()?.toList() ?: emptyList()
        assertEquals(2, parts[2].meta["value"].int)
        println(reconstructed.data!!.size)
    }

}