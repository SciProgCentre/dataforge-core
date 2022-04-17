package space.kscience.dataforge.workspace

import io.ktor.utils.io.core.Input
import io.ktor.utils.io.core.Output
import kotlinx.coroutines.runBlocking
import space.kscience.dataforge.context.Global
import space.kscience.dataforge.data.*
import space.kscience.dataforge.io.IOFormat
import space.kscience.dataforge.io.io
import space.kscience.dataforge.io.readUtf8String
import space.kscience.dataforge.io.writeUtf8String
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.misc.DFExperimental
import java.nio.file.Files
import java.nio.file.Path
import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals


class FileDataTest {
    val dataNode = runBlocking {
        DataTree<String> {
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

    object StringFormatResolver : FileFormatResolver<String> {
        override val type: KType = typeOf<String>()

        override fun invoke(path: Path, meta: Meta): IOFormat<String> = StringIOFormat

    }

    @Test
    @DFExperimental
    fun testDataWriteRead() {
        Global.io.run {
            val dir = Files.createTempDirectory("df_data_node")
            runBlocking {
                writeDataDirectory(dir, dataNode, StringIOFormat)
                println(dir.toUri().toString())
                val reconstructed = readDataDirectory(dir, StringFormatResolver)
                assertEquals(dataNode.getData("dir.a")?.meta, reconstructed.getData("dir.a")?.meta)
                assertEquals(dataNode.getData("b")?.await(), reconstructed.getData("b")?.await())
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
                println(zip.toUri().toString())
                val reconstructed = readDataDirectory(zip, StringFormatResolver)
                assertEquals(dataNode.getData("dir.a")?.meta, reconstructed.getData("dir.a")?.meta)
                assertEquals(dataNode.getData("b")?.await(), reconstructed.getData("b")?.await())
            }
        }
    }
}