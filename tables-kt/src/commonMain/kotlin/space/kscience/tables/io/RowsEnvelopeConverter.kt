package space.kscience.tables.io

import space.kscience.dataforge.io.Envelope
import space.kscience.tables.Rows

/**
 * [Rows] to Envelope converter.
 */
public interface RowsEnvelopeConverter<T> {
    public fun readRows(envelope: Envelope): Rows<T>

    public fun writeRows(rows: Rows<T>): Envelope
}

