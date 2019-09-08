package hep.dataforge.io.tcp

import hep.dataforge.context.Global
import hep.dataforge.io.Envelope
import hep.dataforge.io.Responder
import kotlinx.coroutines.GlobalScope
import org.junit.AfterClass
import org.junit.BeforeClass
import kotlin.time.ExperimentalTime

object EchoResponder : Responder {
    override suspend fun respond(request: Envelope): Envelope = request
}

@ExperimentalTime
class EnvelopeServerTest {
    companion object {
        @JvmStatic
        val echoEnvelopeServer = EnvelopeServer(Global, 7778, EchoResponder, GlobalScope)

        @BeforeClass
        @JvmStatic
        fun start() {
            echoEnvelopeServer.start()
        }

        @AfterClass
        @JvmStatic
        fun close() {
            echoEnvelopeServer.stop()
        }
    }


//    @Test
//    fun doEchoTest() {
//        val client = EnvelopeClient(Global, host = "localhost", port = 7778)
//        val request = Envelope.build {
//            type = "test.echo"
//            meta {
//                "test.value" to 22
//            }
//            data {
//                writeDouble(22.7)
//            }
//        }
//        val response = runBlocking {
//            client.respond(request)
//        }
//
//        assertEquals(request.meta, response.meta)
//        assertEquals(request.data, response.data)
//
//        runBlocking {
//            client.close()
//        }
//    }
}