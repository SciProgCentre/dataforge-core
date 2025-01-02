package space.kscience.dataforge.workspace

import kotlinx.coroutines.test.runTest
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.readString
import kotlinx.io.writeString
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.Global
import space.kscience.dataforge.data.*
import space.kscience.dataforge.io.*
import space.kscience.dataforge.io.yaml.YamlPlugin
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.misc.DFExperimental
import java.nio.file.Files
import kotlin.io.path.deleteExisting
import kotlin.io.path.fileSize
import kotlin.io.path.toPath
import kotlin.test.Test
import kotlin.test.assertEquals


class FileDataTest {
    val dataNode = DataTree.static<String> {
        node("dir") {
            value("a", "Some string") {
                "content" put "Some string"
            }
        }
        value("b", "root data")
//        meta {
//            "content" put "This is root meta node"
//        }
    }


    object StringIOFormat : IOFormat<String> {

        override fun writeTo(sink: Sink, obj: String) {
            sink.writeString(obj)
        }

        override fun readFrom(source: Source): String = source.readString()
    }

    @Test
    @DFExperimental
    fun testDataWriteRead() = runTest {
        val io = Global.io
        val dir = Files.createTempDirectory("df_data_node")
        io.writeDataDirectory(dir, dataNode, StringIOFormat)
        println(dir.toUri().toString())
        val data = io.readDirectory(dir)
        val reconstructed = data.transformEach(this) { (_, value) ->
            value.toByteArray().decodeToString()
        }
        assertEquals(dataNode["dir.a"]?.meta?.get("content"), reconstructed["dir.a"]?.meta?.get("content"))
        assertEquals(dataNode["b"]?.await(), reconstructed["b"]?.await())
    }


    @Test
    @DFExperimental
    fun testZipWriteRead() = runTest {
        val io = Global.io
        val zip = Files.createTempFile("df_data_node", ".zip")
        zip.deleteExisting()
        io.writeZip(zip, dataNode, StringIOFormat)
        println(zip.toUri().toString())
        val reconstructed = io.readDirectory(zip).transformEach(this) { (_, value) ->
            value.toByteArray().decodeToString()
        }
        assertEquals(dataNode["dir.a"]?.meta?.get("content"), reconstructed["dir.a"]?.meta?.get("content"))
        assertEquals(dataNode["b"]?.await(), reconstructed["b"]?.await())

    }

    @OptIn(DFExperimental::class)
    @Test
    fun testNonEnvelope() {
        val context = Context {
            plugin(YamlPlugin)
        }
        val resource = javaClass.classLoader.getResource("SPC.png")!!
        val data: Envelope = context.io.readEnvelopeFile(resource.toURI().toPath(), true)
        assertEquals(resource.toURI().toPath().fileSize(), data.data?.size?.toLong())
    }
}