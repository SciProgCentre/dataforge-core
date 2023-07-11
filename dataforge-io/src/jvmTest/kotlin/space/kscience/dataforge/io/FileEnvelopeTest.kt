package space.kscience.dataforge.io

import space.kscience.dataforge.context.Global
import space.kscience.dataforge.misc.DFExperimental
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertTrue


@DFExperimental
class FileEnvelopeTest {
    val envelope = Envelope {
        meta {
            "a" put "AAA"
            "b" put 22.2
        }
        dataType = "space.kscience.dataforge.test"
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
            val restored: Envelope = readEnvelopeFile(tmpPath)
            assertTrue { envelope.contentEquals(restored) }
        }
    }

    @Test
    fun testFileWriteReadTagless() {
        Global.io.run {
            val tmpPath = Files.createTempFile("dataforge_test_tagless", ".df")
            writeEnvelopeFile(tmpPath, envelope, envelopeFormat = TaglessEnvelopeFormat)
            println(tmpPath.toUri())
            val restored: Envelope = readEnvelopeFile(tmpPath)
            assertTrue { envelope.contentEquals(restored) }
        }
    }
}