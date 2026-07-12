package space.kscience.tables.io

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import space.kscience.dataforge.io.Envelope
import space.kscience.dataforge.io.asBinary
import space.kscience.dataforge.io.toByteArray
import space.kscience.dataforge.meta.*
import space.kscience.dataforge.names.getIndexedList
import space.kscience.dataforge.names.parseAsName
import space.kscience.tables.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterInputStream
import kotlin.reflect.KType

/**
 * A class that provides functionality to convert `Rows<T>` objects into an `Envelope` format
 * and vice versa while supporting compressed serialization and deserialization of the data.
 *
 * @param T The type of the data being handled within the rows.
 * @property converter A `MetaConverter` implementation responsible for converting row values
 *                     to and from metadata during serialization and deserialization.
 * @property type The `KType` representing the type of data being processed.
 */
public class ZipRowsEnvelopeConverter<T>(
    public val converter: MetaConverter<T>,
    public val type: KType
) : RowsEnvelopeConverter<T> {


    @OptIn(ExperimentalSerializationApi::class)
    override fun writeRows(rows: Rows<T>): Envelope {
        val headerMeta = Meta {
            rows.headers.forEach { header ->
                append("column", Meta {
                    "name" put header.name
                    header.meta.takeIf { !it.isEmpty() }.let {
                        "meta" put header.meta
                    }
                })
            }
        }
        val meta = Meta {
            "header" put headerMeta
        }

        val rowsPrepared = rows.rowSequence().map { row ->
            val map = if (row is MapRow) row.values else rows.headers.associate { it.name to row.getOrNull(it.name) }
            map.mapValues { it.value?.let { value -> converter.convert(value) } ?: Meta.EMPTY }
        }.toList()

        val baos = ByteArrayOutputStream()

        val zipOutputStream = DeflaterOutputStream(baos)
        Json.encodeToStream(rowsPrepared, zipOutputStream)
        zipOutputStream.finish()
        return Envelope(meta, baos.toByteArray().asBinary())
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun readRows(envelope: Envelope): Rows<T> {
        val header: TableHeader<T> = envelope.meta.getIndexedList("header.column".parseAsName()).map { item ->
            SimpleColumnHeader(item["name"].string ?: "default", type, item["meta"] ?: Meta.EMPTY)
        }
        val bais = ByteArrayInputStream(envelope.data?.toByteArray() ?: error("No data in envelope"))
        val zipInputStream = InflaterInputStream(bais)
        val dao = Json.decodeFromStream<List<Map<String, Meta>>>(zipInputStream)
        zipInputStream.close()
        val rows = dao.map { m ->
            MapRow(m.mapValues { converter.read(it.value) })
        }

        return RowTable(header, rows)
    }

}