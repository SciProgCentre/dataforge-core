package hep.dataforge.workspace

import hep.dataforge.context.Global
import hep.dataforge.data.*
import hep.dataforge.io.IOFormat
import hep.dataforge.io.io
import hep.dataforge.meta.DFExperimental
import hep.dataforge.meta.Meta
import kotlinx.coroutines.runBlocking
import kotlinx.io.Input
import kotlinx.io.Output
import kotlinx.io.text.readUtf8String
import kotlinx.io.text.writeUtf8String
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals


class FileDataTest {
    val dataNode = DataTree<String> {
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
        override fun writeObject(output: Output, obj: String) {
            output.writeUtf8String(obj)
        }

        override fun readObject(input: Input): String {
            return input.readUtf8String()
        }

        override fun toMeta(): Meta = Meta {
            IOFormat.NAME_KEY put "string"
        }

    }

    @Test
    @DFExperimental
    fun testDataWriteRead() {
        Global.io.run {
            val dir = Files.createTempDirectory("df_data_node")
            runBlocking {
                writeDataDirectory(dir, dataNode, StringIOFormat)
            }
            println(dir.toUri().toString())
            val reconstructed = readDataDirectory(dir, String::class) { _, _ -> StringIOFormat }
            assertEquals(dataNode["dir.a"]?.meta, reconstructed["dir.a"]?.meta)
            assertEquals(dataNode["b"]?.data?.get(), reconstructed["b"]?.data?.get())
        }
    }


    @Test
    @DFExperimental
    fun testZipWriteRead() {
        Global.io.run {
            val zip = Files.createTempFile("df_data_node", ".zip")
            runBlocking {
                writeZip(zip, dataNode, StringIOFormat)
            }
            println(zip.toUri().toString())
            val reconstructed = readDataDirectory(zip, String::class) { _, _ -> StringIOFormat }
            assertEquals(dataNode["dir.a"]?.meta, reconstructed["dir.a"]?.meta)
            assertEquals(dataNode["b"]?.data?.get(), reconstructed["b"]?.data?.get())
        }
    }
}