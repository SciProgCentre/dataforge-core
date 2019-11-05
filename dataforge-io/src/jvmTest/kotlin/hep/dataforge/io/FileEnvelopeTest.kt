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
    fun testFileWriteTagged() {
        val tmpPath = Files.createTempFile("dataforge_test", ".df")
        Global.io.writeEnvelopeFile(tmpPath, envelope)
        assertTrue { tmpPath.toFile().length() > 0 }
    }

    @Test
    fun testFileWriteReadTagged() {
        val tmpPath = Files.createTempFile("dataforge_test", ".df")
        Global.io.writeEnvelopeFile(tmpPath,envelope)
        println(tmpPath.toUri())
        val restored: Envelope = Global.io.readEnvelopeFile(tmpPath)
        assertTrue { envelope.contentEquals(restored) }
    }

    @Test
    fun testFileWriteTagless() {
        val tmpPath = Files.createTempFile("dataforge_test", ".df")
        Global.io.writeEnvelopeFile(tmpPath, envelope, formatFactory = TaglessEnvelopeFormat)
        assertTrue { tmpPath.toFile().length() > 0 }
    }

    @Test
    fun testFileWriteReadTagless() {
        val tmpPath = Files.createTempFile("dataforge_test", ".df")
        Global.io.writeEnvelopeFile(tmpPath, envelope, formatFactory = TaglessEnvelopeFormat)
        println(tmpPath.toUri())
        val restored: Envelope = Global.io.readEnvelopeFile(tmpPath, formatFactory = TaglessEnvelopeFormat)
        assertTrue { envelope.contentEquals(restored) }
    }
}