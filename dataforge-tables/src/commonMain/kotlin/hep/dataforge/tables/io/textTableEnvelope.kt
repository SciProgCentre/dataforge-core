package hep.dataforge.tables.io

import hep.dataforge.io.Envelope
import hep.dataforge.meta.*
import hep.dataforge.tables.SimpleColumnHeader
import hep.dataforge.tables.Table
import hep.dataforge.values.Value
import kotlinx.io.Binary
import kotlinx.io.ByteArrayOutput
import kotlinx.io.ExperimentalIoApi
import kotlinx.io.asBinary


@ExperimentalIoApi
suspend fun Table<Value>.wrap(): Envelope = Envelope {
    meta {
        header.forEachIndexed { index, columnHeader ->
            set("column", index.toString(), Meta {
                "name" put columnHeader.name
                if (!columnHeader.meta.isEmpty()) {
                    "meta" put columnHeader.meta
                }
            })
        }
    }

    type = "table.value"
    dataID = "valueTable[${this@wrap.hashCode()}]"

    data = ByteArrayOutput().apply { writeRows(this@wrap) }.toByteArray().asBinary()
}

@DFExperimental
@ExperimentalIoApi
fun TextRows.Companion.readEnvelope(envelope: Envelope): TextRows {
    val header = envelope.meta.getIndexed("column")
        .entries.sortedBy { it.key.toInt() }
        .map { (_, item) ->
            SimpleColumnHeader(item.node["name"].string!!, Value::class, item.node["meta"].node ?: Meta.EMPTY)
        }
    return TextRows(header, envelope.data ?: Binary.EMPTY)
}