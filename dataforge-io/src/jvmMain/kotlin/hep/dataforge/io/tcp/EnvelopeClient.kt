package hep.dataforge.io.tcp

import hep.dataforge.context.Context
import hep.dataforge.context.ContextAware
import hep.dataforge.io.*
import hep.dataforge.meta.EmptyMeta
import hep.dataforge.meta.Meta
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.io.streams.writePacket
import java.net.Socket
import java.util.concurrent.Executors
import kotlin.time.ExperimentalTime

@ExperimentalTime
class EnvelopeClient(
    override val context: Context,
    val host: String,
    val port: Int,
    formatFactory: EnvelopeFormatFactory = TaggedEnvelopeFormat,
    formatMeta: Meta = EmptyMeta
) : Responder, ContextAware {

    private val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    private val format = formatFactory(formatMeta, context = context)

//    private var socket: SocketChannel? = null
//
//    private fun getSocket(): Socket {
//        val socket = socket ?: Socket(host, port).also { this.socket = it }
//        return if (socket.isConnected) {
//            socket
//        } else {
//            Socket(host, port).also { this.socket = it }
//        }
//    }

    suspend fun close() {
        try {
            respond(
                Envelope.invoke {
                    type = EnvelopeServer.SHUTDOWN_ENVELOPE_TYPE
                }
            )
        } catch (ex: Exception) {
            logger.error { ex }
        }
    }


    override suspend fun respond(request: Envelope): Envelope = withContext(dispatcher) {
        //val address = InetSocketAddress(host,port)
        val socket = Socket(host, port)
        val input = socket.getInputStream().asInput()
        val output = socket.getOutputStream()
        format.run {
            output.writePacket {
                writeObject(request)
            }
            logger.debug { "Sent request with type ${request.type} to ${socket.remoteSocketAddress}" }
            val res = input.readObject()
            logger.debug { "Received response with type ${res.type} from ${socket.remoteSocketAddress}" }
            return@withContext res
        }
    }
}