package space.kscience.dataforge.tables.io

import space.kscience.dataforge.io.Binary
import space.kscience.dataforge.io.Envelope
import space.kscience.dataforge.io.asBinary
import space.kscience.dataforge.io.buildByteArray
import space.kscience.dataforge.meta.*
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.tables.SimpleColumnHeader
import space.kscience.dataforge.tables.Table
import space.kscience.dataforge.values.Value
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