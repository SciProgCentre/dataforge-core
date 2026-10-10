package space.kscience.dataforge.io

import kotlinx.serialization.SerializationException
import space.kscience.dataforge.context.Global
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.put
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class FileMetaDirectoryTest {
    private val meta = Meta {
        "name" put "sample"
        "count" put 3
    }

    private fun withDirectory(block: (Path) -> Unit) {
        val directory = Files.createTempDirectory("dataforge-meta-")
        try {
            block(directory)
        } finally {
            Files.list(directory).use { files -> files.forEach { Files.delete(it) } }
            Files.delete(directory)
        }
    }

    @Test
    fun strictDirectoryReadRoundTripsWrittenMeta() = withDirectory { directory ->
        Global.io.writeMetaFile(directory, meta)
        assertEquals(meta, Global.io.readMetaFile(directory.resolve("@meta.json")))
        assertEquals(meta, Global.io.readMetaFile(directory))
    }

    @Test
    fun nullableDirectoryReadRoundTripsWrittenMeta() = withDirectory { directory ->
        Global.io.writeMetaFile(directory, meta)
        assertEquals(meta, Global.io.readMetaFileOrNull(directory.resolve("@meta.json")))
        assertEquals(meta, Global.io.readMetaFileOrNull(directory))
    }

    @Test
    fun missingMetadataKeepsStrictAndNullableResults() = withDirectory { directory ->
        val missing = directory.resolve("missing.json")
        assertNull(Global.io.readMetaFileOrNull(missing))
        assertFailsWith<IllegalStateException> { Global.io.readMetaFile(missing) }
        Global.io.writeMetaFile(directory.resolve("ordinary.json"), meta)
        assertEquals(meta, Global.io.readMetaFile(directory.resolve("ordinary.json")))
        assertNull(Global.io.readMetaFileOrNull(directory))
        assertFailsWith<IllegalStateException> { Global.io.readMetaFile(directory) }
    }

    @Test
    fun formatOverrideReadsUnknownExtension() = withDirectory { directory ->
        Global.io.writeMetaFile(directory.resolve("@meta.unknown"), meta)
        assertNull(Global.io.readMetaFileOrNull(directory))
        assertFailsWith<IllegalStateException> { Global.io.readMetaFile(directory) }
        assertEquals(meta, Global.io.readMetaFile(directory, JsonMetaFormat))
        assertEquals(meta, Global.io.readMetaFileOrNull(directory, JsonMetaFormat))
    }

    @Test
    fun malformedMetadataIsNotConvertedToNull() = withDirectory { directory ->
        Files.writeString(directory.resolve("@meta.json"), "{")
        assertFailsWith<SerializationException> { Global.io.readMetaFile(directory) }
        assertFailsWith<SerializationException> { Global.io.readMetaFileOrNull(directory) }
    }
}
