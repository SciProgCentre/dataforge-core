package hep.dataforge.envelopes

import hep.dataforge.meta.Meta
import kotlinx.io.core.IoBuffer

interface Envelope{
    val meta: Meta
    val data: IoBuffer
}