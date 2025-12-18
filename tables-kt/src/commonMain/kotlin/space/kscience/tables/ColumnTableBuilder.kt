package space.kscience.tables

/**
 * A table with columns that could be reordered. Column content could not be changed after creation.
 */
public class ColumnTableBuilder<T>(
    public val rowsSize: Int,
    private val _columns: MutableList<Column<T>> = ArrayList(),
) : ColumnTable<T>(_columns) {

    override val rows: List<Row<T>>
        get() = (0 until rowsSize).map {
            VirtualRow(this, it)
        }

    override fun getOrNull(row: Int, column: String): T? = columns.getOrNull(column)?.getOrNull(row)

    /**
     * Add or insert at [index] a fixed column
     */
    public fun addColumn(column: Column<T>, index: Int? = null) {
        require(column.size == this.rowsSize) { "Required column size $rowsSize, but found ${column.size}" }
        require(_columns.find { it.name == column.name } == null) { "Column with name ${column.name} already exists" }
        if (index == null) {
            _columns.add(column)
        } else {
            _columns.add(index, column)
        }
    }

    /**
     * Remove a column
     */
    public fun removeColumn(name: String) {
        _columns.removeAll { it.name == name }
    }

    /**
     * Get or set values for given column. The size of column must be the same as table [rowsNum]
     */
    public var <R : T> ColumnHeader<R>.values: Collection<R?>
        get() = columns[this].listValues()
        set(value) {
            val newColumn = ListColumn(this, value.toList())
            removeColumn(name)
            addColumn(newColumn)
        }
}

/**
 * Set or replace column using given [expression]
 */
public fun <T, R : T> ColumnTableBuilder<T>.transform(
    header: ColumnHeader<R>,
    index: Int? = null,
    expression: (Row<T>) -> R,
) {
    val column = rowsToColumn(header, false, expression)
    removeColumn(header.name)
    addColumn(column, index)
}

/**
 * Set or replace column using column name
 */
public inline fun <T, reified R : T> ColumnTableBuilder<T>.transform(
    name: String,
    index: Int? = null,
    noinline expression: (Row<T>) -> R,
): Unit = transform(ColumnHeader<R>(name), index, expression)

/**
 * Adds or replaces a column in the ColumnTableBuilder with the given header and data.
 *
 * @param header the header of the column to be added or replaced
 * @param data the data for the column
 */
public fun <T, R : T> ColumnTableBuilder<T>.column(header: ColumnHeader<R>, data: Iterable<R>) {
    removeColumn(header.name)
    val column = ListColumn(header.name, data.toList(), header.type, header.meta)
    addColumn(column)
}

/**
 * Adds a column with the given header to the table. Optionally, the column can be inserted at a specific index.
 * The column is filled with data using the provided data builder function.
 *
 * @param header The header of the column to be added.
 * @param index The index at which the column should be inserted. If null, the column is added to the end of the table.
 * @param dataBuilder A function that takes an index and returns the data to fill the column at that index.
 * @return The newly added column.
 */
public fun <T, R : T> ColumnTableBuilder<T>.fill(header: ColumnHeader<R>, index: Int? = null, dataBuilder: (Int) -> R?): Column<R> {
    //TODO use specialized columns if possible
    val column = ListColumn(header, rowsSize, dataBuilder)
    addColumn(column, index)
    return column
}

/**
 * Shallow copy table to a new [ColumnTableBuilder]
 */
public fun <T> ColumnTable<T>.builder(): ColumnTableBuilder<T> =
    ColumnTableBuilder<T>(rowsSize, columns.toMutableList())


/**
 * Shallow copy and edit [Table] and edit it as [ColumnTable]
 */
public fun <T> Table<T>.withColumns(block: ColumnTableBuilder<T>.() -> Unit): ColumnTable<T> =
    ColumnTableBuilder<T>(rowsSize, columns.toMutableList()).apply(block)