package hep.dataforge.tables

/**
 * Mutable table with a fixed size, but dynamic columns
 */
class MutableColumnTable<C: Any>(val size: Int) : Table<C> {
    private val _columns = ArrayList<Column<C>>()

    override val columns: List<Column<C>> get() = _columns
    override val rows: List<Row<C>> get() = (0 until size).map {
        VirtualRow(this, it)
    }

    override fun getValue(row: Int, column: String): C? = columns[column]?.get(row)

    /**
     * Add a fixed column to the end of the table
     */
    fun add(column: Column<C>) {
        require(column.size == this.size) { "Required column size $size, but found ${column.size}" }
        _columns.add(column)
    }

    /**
     * Insert a column at [index]
     */
    fun insert(index: Int, column: Column<C>) {
        require(column.size == this.size) { "Required column size $size, but found ${column.size}" }
        _columns.add(index, column)
    }
}
