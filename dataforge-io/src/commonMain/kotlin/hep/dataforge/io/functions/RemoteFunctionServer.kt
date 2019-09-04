package hep.dataforge.io.functions

import hep.dataforge.context.Context
import hep.dataforge.context.ContextAware
import hep.dataforge.io.*
import hep.dataforge.meta.get
import hep.dataforge.meta.int
import hep.dataforge.meta.node
import hep.dataforge.meta.string

class RemoteFunctionServer(
    override val context: Context,
    val functionServer: FunctionServer
) : ContextAware, Responder {

    private val plugin by lazy {
        context.plugins.load(FunctionsPlugin)
    }

    override suspend fun respond(request: Envelope): Envelope {
        require(request.type == RemoteFunctionClient.REQUEST_TYPE) { "Unexpected message type: ${request.type}" }
        val functionName = request.meta[RemoteFunctionClient.FUNCTION_NAME_KEY].string ?: ""

        @Suppress("UNCHECKED_CAST") val spec = request.meta[RemoteFunctionClient.FUNCTION_SPEC_KEY].node?.let {
            plugin.resolve(it) as FunctionSpec<Any, Any>
        } ?: error("Function specification not found")

        val size = request
            .meta[RemoteFunctionClient.SIZE_KEY].int ?: 1

        val input = request.data?.read {
            spec.inputFormat.run {
                List(size) {
                    readThis()
                }
            }
        } ?: error("Input is empty")

        val output = functionServer.callMany<Any, Any>(functionName, spec, input)

        return Envelope.build {
            type = RemoteFunctionClient.RESPONSE_TYPE
            meta {
                RemoteFunctionClient.FUNCTION_NAME_KEY to functionName
                RemoteFunctionClient.FUNCTION_SPEC_KEY to spec.toMeta()
                RemoteFunctionClient.SIZE_KEY to output.size
            }
            data {
                spec.outputFormat.run {
                    output.forEach {
                        writeThis(it)
                    }
                }
            }

        }
    }
}