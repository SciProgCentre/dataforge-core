package space.kscience.tables.csv

import com.jsoizo.kotlincsv.CsvDialect
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import space.kscience.dataforge.meta.Value
import space.kscience.tables.RowTable
import space.kscience.tables.Table
import space.kscience.tables.get
import space.kscience.tables.valueRow

internal class StringReadWrite {
    val table = RowTable<Value> {
        val a by column<Value>()
        val b by column<Value>()
        valueRow(a to 1, b to "b1")
        valueRow(a to 2, b to "b2")
    }

    @Test
    fun writeRead() {
        val string = Table.writeCsvString(table)
        println(string)
        val reconstructed = Table.readCsvString(string)

        assertEquals("b2", reconstructed[1, "b"])
    }

    @Test
    fun writeReadTsv() {
        val string = Table.writeCsvString(table) { dialect = CsvDialect.TSV }
        println(string)
        val reconstructed = Table.readCsvString(string){ dialect = CsvDialect.TSV }

        assertEquals("b2", reconstructed[1, "b"])
    }
}