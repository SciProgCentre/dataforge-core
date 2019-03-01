package hep.dataforge.meta.io

import hep.dataforge.meta.Meta
import hep.dataforge.meta.get
import hep.dataforge.meta.string

interface Envelope {
    val meta: Meta
    val data: Binary?

    companion object {
//        /**
//         * Property keys
//         */
//        const val TYPE_PROPERTY = "type"
//        const val META_TYPE_PROPERTY = "metaType"
//        const val META_LENGTH_PROPERTY = "metaLength"
//        const val DATA_LENGTH_PROPERTY = "dataLength"

        /**
         * meta keys
         */
        const val ENVELOPE_NODE = "@envelope"
        const val ENVELOPE_TYPE_KEY = "$ENVELOPE_NODE.type"
        const val ENVELOPE_DATA_TYPE_KEY = "$ENVELOPE_NODE.dataType"
        const val ENVELOPE_DESCRIPTION_KEY = "$ENVELOPE_NODE.description"
        //const val ENVELOPE_TIME_KEY = "@envelope.time"
    }
}

/**
 * The purpose of the envelope
 *
 * @return
 */
val Envelope.type: String? get() = meta[Envelope.ENVELOPE_TYPE_KEY].string

/**
 * The type of data encoding
 *
 * @return
 */
val Envelope.dataType: String? get() = meta[Envelope.ENVELOPE_DATA_TYPE_KEY].string

/**
 * Textual user friendly description
 *
 * @return
 */
val Envelope.description: String? get() = meta[Envelope.ENVELOPE_DESCRIPTION_KEY].string