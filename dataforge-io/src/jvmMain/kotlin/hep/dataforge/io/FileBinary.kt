package hep.dataforge.io

import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.Input
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption

class FileBinary(val path: Path, private val offset: Int = 0) : RandomAccessBinary {
    override fun <R> read(from: Long, size: Long, block: Input.() -> R): R {
        FileChannel.open(path, StandardOpenOption.READ).use {
            val buffer = it.map(FileChannel.MapMode.READ_ONLY, from + offset, size)
            return ByteReadPacket(buffer).block()
        }
    }
}