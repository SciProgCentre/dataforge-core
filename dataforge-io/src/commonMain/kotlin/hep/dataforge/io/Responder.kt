package hep.dataforge.io

/**
 * An object that could respond to external messages asynchronously
 */
public interface Responder {
    /**
     * Send a request and wait for response for this specific request
     */
    public suspend fun respond(request: Envelope): Envelope
}

