package hep.dataforge.tables.io

import hep.dataforge.io.Binary
import hep.dataforge.io.Envelope
import hep.dataforge.io.asBinary
import hep.dataforge.io.buildByteArray
import hep.dataforge.meta.*
import hep.dataforge.misc.DFExperimental
import hep.dataforge.tables.SimpleColumnHeader
import hep.dataforge.tables.Table
import hep.dataforge.values.Value
import kotlin.reflect.typeOf


public suspend fun Table<Value>.toEnvelope(): Envelope = Envelope {
    meta {
        headers.forEachIndexed { index, columnHeader ->
            set("column", index.toString(), Meta {
                "name" put columnHeader.name
                if (!columnHeader.meta.isEmpty()) {
                    "meta" put columnHeader.meta
                }
            })
        }
    }

    type = "table.value"
    dataID = "valueTable[${this@toEnvelope.hashCode()}]"

    data = buildByteArray {
        writeRows(this@toEnvelope)
    }.asBinary()
}

@DFExperimental
public fun TextRows.Companion.readEnvelope(envelope: Envelope): TextRows {
    val header = envelope.meta.getIndexed("column")
        .entries.sortedBy { it.key?.toInt() }
        .map { (_, item) ->
            SimpleColumnHeader<Value>(item.node["name"].string!!, typeOf<Value>(), item.node["meta"].node ?: Meta.EMPTY)
        }
    return TextRows(header, envelope.data ?: Binary.EMPTY)
}