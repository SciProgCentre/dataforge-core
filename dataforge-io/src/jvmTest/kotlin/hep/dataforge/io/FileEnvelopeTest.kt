package hep.dataforge.io

import hep.dataforge.context.Global
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertTrue


class FileEnvelopeTest {
    val envelope = Envelope {
        meta {
            "a" put "AAA"
            "b" put 22.2
        }
        dataType = "hep.dataforge.test"
        dataID = "myData" // добавил только что
        data {
            writeDouble(16.7)

        }
    }

    @Test
    fun testFileWriteRead() {
        Global.io.run {
            val tmpPath = Files.createTempFile("dataforge_test", ".df")
            writeEnvelopeFile(tmpPath, envelope)
            println(tmpPath.toUri())
            val restored: Envelope = readEnvelopeFile(tmpPath)!!
            assertTrue { envelope.contentEquals(restored) }
        }
    }

    @Test
    fun testFileWriteReadTagless() {
        Global.io.run {
            val tmpPath = Files.createTempFile("dataforge_test_tagless", ".df")
            writeEnvelopeFile(tmpPath, envelope, envelopeFormat = TaglessEnvelopeFormat)
            println(tmpPath.toUri())
            val restored: Envelope = readEnvelopeFile(tmpPath)!!
            assertTrue { envelope.contentEquals(restored) }
        }
    }
}