package hep.dataforge.tables

import kotlin.reflect.KClass

class ColumnTable(override val columns: Map<String, Column<*>>) :
    Table {
    private val rowsNum = columns.values.first().size

    init {
        require(columns.values.all { it.size == rowsNum }) { "All columns must be of the same size" }
    }

    override val rows: List<Row>
        get() = (0 until rowsNum).map { VirtualRow(it) }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getValue(row: Int, column: String, type: KClass<out T>): T? {
        val value = columns[column]?.get(row)
        return when {
            value == null -> null
            type.isInstance(value) -> value as T
            else -> error("Expected type is $type, but found ${value::class}")
        }
    }

    private inner class VirtualRow(val index: Int) : Row {
        override fun <T : Any> getValue(column: String, type: KClass<out T>): T? = getValue(index, column, type)
    }
}

class ColumnTableBuilder {
    private val columns = ArrayList<Column<*>>()

    fun build() = ColumnTable(columns.associateBy { it.name })
}