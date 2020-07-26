package hep.dataforge.io

import hep.dataforge.meta.DFExperimental

/**
 * An object that could respond to external messages asynchronously
 */
interface Responder {
    /**
     * Send a request and wait for response for this specific request
     */
    suspend fun respond(request: Envelope): Envelope
}

/**
 * A fire-and-forget consumer of messages
 */
@DFExperimental
interface Consumer {
    fun consume(message: Envelope): Unit
}