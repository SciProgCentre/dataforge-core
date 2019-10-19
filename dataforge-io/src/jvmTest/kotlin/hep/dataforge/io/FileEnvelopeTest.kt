package hep.dataforge.io

import hep.dataforge.context.Global
import java.nio.file.Files
import kotlin.test.Ignore
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
    @Ignore
    fun testFileWriteRead() {
        val tmpPath = Files.createTempFile("dataforge_test", ".df")
        Global.io.writeEnvelopeFile(tmpPath,envelope)
        println(tmpPath.toUri())
        val restored: Envelope = Global.io.readEnvelopeFile(tmpPath)
        assertTrue { envelope.contentEquals(restored) }
    }
}