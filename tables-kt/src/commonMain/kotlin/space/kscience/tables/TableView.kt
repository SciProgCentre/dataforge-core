package space.kscience.tables

/**
 * A read-only view over an existing [Table] that exposes selected or remapped rows and columns.
 *
 * The view delegates value lookup to the origin table after translating the requested row index
 * and column header through its row and column mappings. By default, the view represents the
 * origin table unchanged.
 *
 * Row order, row selection, visible columns, and column aliases can be controlled by the mappings
 * supplied to the constructor. The view does not copy table data; changes in the origin table may
 * be reflected by this view depending on the origin table implementation.
 *
 * @param T the value type stored in the table
 */
public class TableView<T>(
    public val origin: Table<T>,
    private val rowMapping: List<Int> = List(origin.rowsSize) { it },
    private val columnMapping: Map<Column<T>, Column<T>> = origin.columns.associateBy { it }
) : Table<T> {
    override fun getOrNull(row: Int, column: String): T? {
        val originColumn = columns.find { it.name == column }?.let { columnMapping[it] } ?: return null

        return origin.getOrNull(
            row = rowMapping[row],
            column = originColumn.name
        )
    }

    override val columns: Collection<Column<T>>
        get() = columnMapping.keys

    override val rows: List<Row<T>>
        get() {
            val originRows = origin.rows
            return rowMapping.map { originRows[it] }
        }
}