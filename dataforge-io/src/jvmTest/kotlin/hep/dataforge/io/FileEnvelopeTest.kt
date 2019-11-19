package hep.dataforge.io

import hep.dataforge.context.Global
import java.nio.ByteBuffer
import java.nio.file.Files
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail


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
    fun testFileWriteTagged() {
        val tmpPath = Files.createTempFile("dataforge_test", ".df")
        Global.io.writeEnvelopeFile(tmpPath, envelope)
        assertTrue { tmpPath.toFile().length() > 0 }
    }

    @Test
    fun testFileWriteReadTagged() {
        val tmpPath = Files.createTempFile("dataforge_test", ".df")
        Global.io.writeEnvelopeFile(tmpPath, envelope)
        println(tmpPath.toUri())
        val restored: Envelope = Global.io.readEnvelopeFile(tmpPath)!!
        assertTrue { envelope.contentEquals(restored) }
    }

    @Test
    fun testFileWriteTagless() {
        val tmpPath = Files.createTempFile("dataforge_test", ".df")
        Global.io.writeEnvelopeFile(tmpPath, envelope, envelopeFormat = TaglessEnvelopeFormat)
        assertTrue { tmpPath.toFile().length() > 0 }
    }

    @Test
    fun testFileWriteReadTagless() {
        val tmpPath = Files.createTempFile("dataforge_test", ".df")
        Global.io.writeEnvelopeFile(tmpPath, envelope, envelopeFormat = TaglessEnvelopeFormat)
        println(tmpPath.toUri())
        val restored: Envelope = Global.io.readEnvelopeFile(tmpPath)!!
        assertTrue { envelope.contentEquals(restored) }
    }

    @Test
    fun testDataSize() {
        val tmpPath = Files.createTempFile("dataforge_test", ".df")
        Global.io.writeEnvelopeFile(tmpPath, envelope)
        println(tmpPath.toUri())
        val scan = Scanner(tmpPath.toFile().inputStream()).useDelimiter("\n").nextLine()
        println(scan)
        val format = scan.slice(2..5)
        when (format) {
            "DF03" -> {
                val buff = ByteBuffer.allocate(4)
                buff.put(scan.slice(12..19).toByteArray())
                buff.flip()
                val size = buff.long
                println(size)
                assertEquals(8, size)
            }
            "DF02" -> {
                val buff = ByteBuffer.allocate(4)
                buff.put(scan.slice(12..15).toByteArray())
                buff.flip()
                val size = buff.int
                println(size)
                assertEquals(8, size)
            }
            else -> {
                fail("Format $format don't have test")
            }
        }
    }


}