package hep.dataforge.io

interface Responder {
    /**
     * Send a request and wait for response for this specific request
     */
    suspend fun respond(request: Envelope): Envelope
}