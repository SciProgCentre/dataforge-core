package hep.dataforge.io

import hep.dataforge.context.Global
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
        assertEquals(binary?.size?.toInt(), binary?.toBytes()?.size)
    }

    @Test
    fun testFileData(){
        val dataFile = Files.createTempFile("dataforge_test_bin", ".bin")
        dataFile.toFile().writeText("This is my binary")
        val envelopeFromFile = Envelope {
            meta {
                "a" put "AAA"
                "b" put 22.2
            }
            dataType = "hep.dataforge.satellite"
            dataID = "cellDepositTest" // добавил только что
            data = dataFile.asBinary()
        }
        val binary = envelopeFromFile.data!!
        println(binary.toBytes().size)
        assertEquals(binary.size?.toInt(), binary.toBytes().size)

    }

    @Test
    fun testFileDataSizeRewriting() {
        println(System.getProperty("user.dir"))
        val tmpPath = Files.createTempFile("dataforge_test", ".df")
        Global.io.writeEnvelopeFile(tmpPath, envelope)

        val binary = Global.io.readEnvelopeFile(tmpPath)?.data!!
        assertEquals(binary.size.toInt(), binary.toBytes().size)
    }

    @Test
    fun testMultyPartFileData() {
        val envelopeList = (0..5).map {
            val dataFile = Files.createTempFile("dataforge_test_bin_$it", ".bin")
            dataFile.toFile().writeText(DoubleArray(80000) { it.toDouble() }.joinToString())
            val envelopeFromFile = Envelope {
                meta {
                    "a" put "AAA"
                    "b" put 22.2
                }
                dataType = "hep.dataforge.satellite"
                dataID = "cellDepositTest$it" // добавил только что
                data = dataFile.asBinary()
            }
            envelopeFromFile
        }

        val envelope = Envelope {
            multipart(TaggedEnvelopeFormat, envelopeList)
        }
        println(envelopeList.map { it.data?.size }.joinToString(" "))
        println(envelope.data?.size)
        assertTrue { envelope.data!!.size > envelopeList.map { it.data!!.size }.sum() }

    }

}