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
import java.nio.file.Path
import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals


class FileDataTest {
    val dataNode = DataTree.static<String> {
        set("dir") {
            data("a", "Some string") {
                "content" put "Some string"
            }
        }
        data("b", "root data")
        meta {
            "content" put "This is root meta node"
        }
    }

    object StringIOFormat : IOFormat<String> {

        override val type: KType = typeOf<String>()

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

    object StringFormatResolver: FileFormatResolver<String>{
        override val type: KType = typeOf<String>()

        override fun invoke(path: Path, meta: Meta): IOFormat<String> =StringIOFormat

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
            val reconstructed = readDataDirectory(dir,StringFormatResolver)
            runBlocking {
                assertEquals(dataNode.getData("dir.a")?.meta, reconstructed.getData("dir.a")?.meta)
                assertEquals(dataNode.getData("b")?.value(), reconstructed.getData("b")?.value())
            }
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
            val reconstructed = readDataDirectory(zip, StringFormatResolver)
            runBlocking {
                assertEquals(dataNode.getData("dir.a")?.meta, reconstructed.getData("dir.a")?.meta)
                assertEquals(dataNode.getData("b")?.value(), reconstructed.getData("b")?.value())
            }
        }
    }
}