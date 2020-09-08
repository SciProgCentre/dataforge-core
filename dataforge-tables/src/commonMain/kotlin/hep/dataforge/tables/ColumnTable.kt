package hep.dataforge.tables

/**
 * @param T bottom type for all columns in the table
 */
public class ColumnTable<T : Any>(override val columns: Collection<Column<T>>) : Table<T> {
    private val rowsNum = columns.first().size

    init {
        require(columns.all { it.size == rowsNum }) { "All columns must be of the same size" }
    }

    override val rows: List<Row<T>>
        get() = (0 until rowsNum).map { VirtualRow(this, it) }

    override fun getValue(row: Int, column: String): T? = columns[column]?.get(row)
}

internal class VirtualRow<T : Any>(val table: Table<T>, val index: Int) : Row<T> {
    override fun getValue(column: String): T? = table.getValue(index, column)

//    override fun <T : C> get(columnHeader: ColumnHeader<T>): T? {
//        return table.co[columnHeader][index]
//    }
}

