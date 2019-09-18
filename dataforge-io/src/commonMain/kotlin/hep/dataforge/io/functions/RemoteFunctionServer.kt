package hep.dataforge.io.functions

import hep.dataforge.context.Context
import hep.dataforge.context.ContextAware
import hep.dataforge.io.Envelope
import hep.dataforge.io.IOPlugin
import hep.dataforge.io.Responder
import hep.dataforge.io.type
import hep.dataforge.meta.get
import hep.dataforge.meta.int

class RemoteFunctionServer(
    override val context: Context,
    val functionServer: FunctionServer
) : ContextAware, Responder {

    private val plugin by lazy {
        context.plugins.load(IOPlugin)
    }


    override suspend fun respond(request: Envelope): Envelope {
        require(request.type == RemoteFunctionClient.REQUEST_TYPE) { "Unexpected message type: ${request.type}" }

        val inputFormat = plugin.getInputFormat<Any>(request.meta)
        val outputFormat =  plugin.getOutputFormat<Any>(request.meta)

        val size = request.meta[RemoteFunctionClient.SIZE_KEY].int ?: 1

        val input = request.data?.read {
            inputFormat.run {
                List(size) {
                    readThis()
                }
            }
        } ?: error("Input is empty")

        val output = functionServer.callMany<Any, Any>(
            request.meta,
            input
        )

        return Envelope.invoke {
            meta {
                meta(request.meta)
            }
            type = RemoteFunctionClient.RESPONSE_TYPE
            data {
                outputFormat.run {
                    output.forEach {
                        writeThis(it)
                    }
                }
            }

        }
    }
}