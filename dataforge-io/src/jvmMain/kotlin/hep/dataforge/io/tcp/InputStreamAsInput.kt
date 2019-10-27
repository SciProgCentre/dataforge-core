package hep.dataforge.io.tcp

import kotlinx.io.core.AbstractInput
import kotlinx.io.core.Input
import kotlinx.io.core.IoBuffer
import kotlinx.io.core.IoBuffer.Companion.NoPool
import kotlinx.io.core.writePacket
import kotlinx.io.streams.readPacketAtMost
import java.io.InputStream

/**
 * Modified version of InputStream to Input converter that supports waiting for input
 */
internal class InputStreamAsInput(
    private val stream: InputStream
) : AbstractInput(pool = NoPool) {


    override fun fill(): IoBuffer? {
        val packet = stream.readPacketAtMost(4096)
        return pool.borrow().apply {
            resetForWrite(4096)
            writePacket(packet)
        }
    }

    override fun closeSource() {
        stream.close()
    }
}

fun InputStream.asInput(): Input =
    InputStreamAsInput(this)
