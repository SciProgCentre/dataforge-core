package hep.dataforge.io.tcp

import hep.dataforge.context.Context
import hep.dataforge.context.ContextAware
import hep.dataforge.io.*
import hep.dataforge.meta.Meta
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.net.Socket
import java.util.concurrent.Executors
import kotlin.time.ExperimentalTime

@ExperimentalTime
@Deprecated("To be replaced by flow-based client")
public class EnvelopeClient(
    override val context: Context,
    public val host: String,
    public val port: Int,
    formatFactory: EnvelopeFormatFactory = TaggedEnvelopeFormat,
    formatMeta: Meta = Meta.EMPTY
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

    public suspend fun close() {
        try {
            respond(
                Envelope {
                    type = EnvelopeServer.SHUTDOWN_ENVELOPE_TYPE
                }
            )
        } catch (ex: Exception) {
            logger.error { ex }
        }
    }


    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun respond(request: Envelope): Envelope = withContext(dispatcher) {
        //val address = InetSocketAddress(host,port)
        val socket = Socket(host, port)
        val inputStream = socket.getInputStream()
        val outputStream = socket.getOutputStream()
        format.run {
            outputStream.write {
                writeObject(this, request)
            }
            logger.debug { "Sent request with type ${request.type} to ${socket.remoteSocketAddress}" }
            val res = inputStream.readBlocking { readObject(this) }
            logger.debug { "Received response with type ${res.type} from ${socket.remoteSocketAddress}" }
            return@withContext res
        }
    }
}