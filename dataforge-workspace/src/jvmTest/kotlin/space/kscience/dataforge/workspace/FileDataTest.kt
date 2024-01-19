package space.kscience.dataforge.workspace

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.readString
import kotlinx.io.writeString
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.Global
import space.kscience.dataforge.data.*
import space.kscience.dataforge.io.Envelope
import space.kscience.dataforge.io.IOFormat
import space.kscience.dataforge.io.io
import space.kscience.dataforge.io.readEnvelopeFile
import space.kscience.dataforge.io.yaml.YamlPlugin
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.misc.DFExperimental
import java.nio.file.Files
import kotlin.io.path.fileSize
import kotlin.io.path.toPath
import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals


class FileDataTest {
    val dataNode = GenericDataTree<String> {
        node("dir") {
            static("a", "Some string") {
                "content" put "Some string"
            }
        }
        static("b", "root data")
        meta {
            "content" put "This is root meta node"
        }
    }


    object StringIOFormat : IOFormat<String> {
        override val type: KType get() = typeOf<String>()

        override fun writeTo(sink: Sink, obj: String) {
            sink.writeString(obj)
        }

        override fun readFrom(source: Source): String = source.readString()
    }

    @Test
    @DFExperimental
    fun testDataWriteRead() = with(Global.io) {
        val dir = Files.createTempDirectory("df_data_node")
        runBlocking {
            writeDataDirectory(dir, dataNode, StringIOFormat)
            println(dir.toUri().toString())
            val reconstructed = readDataDirectory(dir) { _, _ -> StringIOFormat }
            assertEquals(dataNode["dir.a"]?.meta?.get("content"), reconstructed["dir.a"]?.meta?.get("content"))
            assertEquals(dataNode["b"]?.await(), reconstructed["b"]?.await())
        }
    }


    @Test
    @DFExperimental
    fun testZipWriteRead() = runTest {
        with(Global.io) {
            val zip = Files.createTempFile("df_data_node", ".zip")
            dataNode.writeZip(zip, StringIOFormat)
            println(zip.toUri().toString())
            val reconstructed = readDataDirectory(zip) { _, _ -> StringIOFormat }
            assertEquals(dataNode["dir.a"]?.meta?.get("content"), reconstructed["dir.a"]?.meta?.get("content"))
            assertEquals(dataNode["b"]?.await(), reconstructed["b"]?.await())
        }
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