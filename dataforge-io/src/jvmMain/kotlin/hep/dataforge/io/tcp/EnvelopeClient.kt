package hep.dataforge.io.tcp

import hep.dataforge.context.Context
import hep.dataforge.context.ContextAware
import hep.dataforge.io.*
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.io.streams.asInput
import kotlinx.io.streams.asOutput
import java.net.Socket
import java.util.concurrent.Executors
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

@ExperimentalTime
class EnvelopeClient(
    override val context: Context,
    val host: String,
    val port: Int,
    val timeout: Duration = 2.seconds,
    val format: EnvelopeFormat = TaggedEnvelopeFormat
) : Responder, ContextAware {

    private val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    private var socket: Socket? = null

    private fun getSocket(): Socket {
        val socket = socket ?: Socket(host, port).also { this.socket = it }
        return if (socket.isConnected) {
            socket
        } else {
            Socket(host, port).also { this.socket = it }
        }
    }

    suspend fun close() {
        respond(
            Envelope.build {
                type = EnvelopeServer.SHUTDOWN_ENVELOPE_TYPE
            }
        )
    }

    override suspend fun respond(request: Envelope): Envelope = withContext(dispatcher) {
        withTimeout(timeout.toLongMilliseconds()) {
            val socket = getSocket()
            val input = socket.getInputStream().asInput()
            val output = socket.getOutputStream().asOutput()
            format.run {
                output.writeThis(request)
                logger.debug { "Sent request with type ${request.type} to ${socket.remoteSocketAddress}" }
                val res = input.readThis()
                logger.debug { "Received response with type ${res.type} from ${socket.remoteSocketAddress}" }
                return@withTimeout res
            }
        }
    }
}