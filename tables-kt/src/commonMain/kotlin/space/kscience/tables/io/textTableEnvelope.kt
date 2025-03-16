package space.kscience.tables.io

import space.kscience.dataforge.io.Binary
import space.kscience.dataforge.io.Envelope
import space.kscience.dataforge.meta.*
import space.kscience.dataforge.names.NameToken
import space.kscience.dataforge.names.asName
import space.kscience.tables.Rows
import space.kscience.tables.SimpleColumnHeader
import space.kscience.tables.Table
import kotlin.reflect.typeOf


/**
 * Convert given [Table] to a TSV-based envelope, encoding header in Meta
 */
public fun Table<Value>.toTextEnvelope(): Envelope = Envelope {
    meta {
        headers.forEachIndexed { index, columnHeader ->
            set(NameToken("column", index.toString()), Meta {
                "name" put columnHeader.name
                if (!columnHeader.meta.isEmpty()) {
                    "meta" put columnHeader.meta
                }
            })
        }
    }

    type = "table.value"
    dataID = "valueTable[${this@toTextEnvelope.hashCode()}]"

    data = Binary {
        writeTextRows(this@toTextEnvelope)
    }
}

/**
 * Read TSV rows from given envelope
 */
public fun Envelope.readTextRows(delimiter: Regex = "\\s+".toRegex()): Rows<Value> {
    val header = meta.getIndexed("column".asName())
        .entries.sortedBy { it.key?.toInt() }
        .map { (_, item) ->
            SimpleColumnHeader<Value>(item["name"].string!!, typeOf<Value>(), item["meta"] ?: Meta.EMPTY)
        }
    return TextRows(header, data ?: Binary.EMPTY, delimiter)
}