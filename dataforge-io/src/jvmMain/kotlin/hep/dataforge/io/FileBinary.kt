package hep.dataforge.io

import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.Input
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

class FileBinary(val path: Path, private val offset: Int = 0) : RandomAccessBinary {

    override val size: ULong
        get() = (Files.size(path) - offset).toULong()


    override fun <R> read(from: UInt, size: UInt, block: Input.() -> R): R {
        FileChannel.open(path, StandardOpenOption.READ).use {
            val buffer = it.map(FileChannel.MapMode.READ_ONLY, (from.toLong() + offset), size.toLong())
            return ByteReadPacket(buffer).block()
        }
    }
}