package hep.dataforge.io

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertTrue


class FileEnvelopeTest {
    val envelope = Envelope {
        meta {
            "a" to "AAA"
            "b" to 22.2
        }
        dataType = "hep.dataforge.test"
        dataID = "myData" // добавил только что
        data {
            writeDouble(16.7)

        }
    }

    @Test
    fun testFileWriteRead() {
        val tmpPath = Files.createTempFile("dataforge_test", ".df")
        tmpPath.writeEnvelope(envelope)
        println(tmpPath.toUri())
        val restored: Envelope = tmpPath.readEnvelope()
        assertTrue { envelope.contentEquals(restored) }
    }
}