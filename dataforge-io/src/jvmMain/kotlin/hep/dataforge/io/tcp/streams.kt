package hep.dataforge.io.tcp

import kotlinx.io.Input
import kotlinx.io.Output
import kotlinx.io.asBinary
import kotlinx.io.buffer.Buffer
import kotlinx.io.buffer.get
import kotlinx.io.buffer.set
import java.io.InputStream
import java.io.OutputStream

private class InputStreamInput(val source: InputStream, val waitForInput: Boolean = false) : Input() {
    override fun closeSource() {
        source.close()
    }

    override fun fill(buffer: Buffer): Int {
        if (waitForInput) {
            while (source.available() == 0) {
                //block until input is available
            }
        }
        var bufferPos = 0
        do {
            val byte = source.read()
            buffer[bufferPos] = byte.toByte()
            bufferPos++
        } while (byte > 0 && bufferPos < buffer.size && source.available() > 0)
        return bufferPos
    }
}

private class OutputStreamOutput(val out: OutputStream) : Output() {
    override fun flush(source: Buffer, length: Int) {
        for (i in 0..length) {
            out.write(source[i].toInt())
        }
        out.flush()
    }

    override fun closeSource() {
        out.flush()
        out.close()
    }
}


fun <R> InputStream.read(size: Int, block: Input.() -> R): R {
    val buffer = ByteArray(size)
    read(buffer)
    return buffer.asBinary().read(block)
}

fun <R> InputStream.read(block: Input.() -> R): R =
    InputStreamInput(this, false).block()

fun <R> InputStream.readBlocking(block: Input.() -> R): R =
    InputStreamInput(this, true).block()

fun OutputStream.write(block: Output.() -> Unit) {
    OutputStreamOutput(this).block()
}