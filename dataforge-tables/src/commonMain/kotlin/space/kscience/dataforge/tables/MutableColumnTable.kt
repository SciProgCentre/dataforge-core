package space.kscience.dataforge.tables

/**
 * Mutable table with a fixed size, but dynamic columns
 */
public class MutableColumnTable<C: Any>(public val size: Int) : Table<C> {
    private val _columns = ArrayList<Column<C>>()

    override val columns: List<Column<C>> get() = _columns
    override val rows: List<Row<C>> get() = (0 until size).map {
        VirtualRow(this, it)
    }

    override fun get(row: Int, column: String): C? = columns[column]?.get(row)

    /**
     * Add a fixed column to the end of the table
     */
    public fun add(column: Column<C>) {
        require(column.size == this.size) { "Required column size $size, but found ${column.size}" }
        _columns.add(column)
    }

    /**
     * Insert a column at [index]
     */
    public fun insert(index: Int, column: Column<C>) {
        require(column.size == this.size) { "Required column size $size, but found ${column.size}" }
        _columns.add(index, column)
    }
}
