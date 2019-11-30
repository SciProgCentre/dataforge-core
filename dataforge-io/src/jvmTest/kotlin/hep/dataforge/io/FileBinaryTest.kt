package hep.dataforge.io

import hep.dataforge.context.Global
import kotlinx.io.asBinary
import kotlinx.io.toByteArray
import kotlinx.io.writeDouble
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class FileBinaryTest {
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
    fun testSize() {
        val binary = envelope.data
        assertEquals(binary?.size?.toInt(), binary?.toByteArray()?.size)
    }

    @Test
    fun testFileData() {
        val dataFile = Files.createTempFile("dataforge_test_bin", ".bin")
        dataFile.toFile().writeText("This is my binary")
        val envelopeFromFile = Envelope {
            meta {
                "a" put "AAA"
                "b" put 22.2
            }
            dataType = "hep.dataforge.satellite"
            dataID = "cellDepositTest"
            data = dataFile.asBinary()
        }
        val binary = envelopeFromFile.data!!
        println(binary.toByteArray().size)
        assertEquals(binary.size.toInt(), binary.toByteArray().size)

    }

    @Test
    fun testFileDataSizeRewriting() {
        println(System.getProperty("user.dir"))
        val tmpPath = Files.createTempFile("dataforge_test", ".df")
        Global.io.writeEnvelopeFile(tmpPath, envelope)

        val binary = Global.io.readEnvelopeFile(tmpPath)?.data!!
        assertEquals(binary.size.toInt(), binary.toByteArray().size)
    }
}