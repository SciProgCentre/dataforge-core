package hep.dataforge.io

import hep.dataforge.context.Global
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.assertEquals

class BinaryTest {
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
        val binary = envelope.data!!
        assertEquals(binary.size.toInt(), binary.toBytes().size)
    }


    val envelopeFromFile = Envelope {
        meta {
            "a" put "AAA"
            "b" put 22.2
        }
        dataType = "hep.dataforge.satellite"
        dataID = "cellDepositTest" // добавил только что
        this@Envelope.data = Paths.get("test_data", "binaryBlock.bin").asBinary()
    }

    @Test
    fun testFileDataSize() {
        println(System.getProperty("user.dir"))
        val binary = envelopeFromFile.data!!
        println(binary.toBytes().size)
        assertEquals(binary.size.toInt(), binary.toBytes().size)
    }

    @Test
    fun testFileDataSizeRewriting() {
        println(System.getProperty("user.dir"))
        val tmpPath = Files.createTempFile("dataforge_test", ".df")
        Global.io.writeEnvelopeFile(tmpPath, envelopeFromFile)

        val binary = Global.io.readEnvelopeFile(tmpPath).data!!
        assertEquals(binary.size.toInt(), binary.toBytes().size)
    }

}