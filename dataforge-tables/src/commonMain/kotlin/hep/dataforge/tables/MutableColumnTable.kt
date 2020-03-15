package hep.dataforge.tables

import hep.dataforge.meta.Meta
import kotlin.reflect.KClass

/**
 * Mutable table with a fixed size, but dynamic columns
 */
class MutableColumnTable<C: Any>(val size: Int) : Table<C> {
    private val _columns = ArrayList<Column<C>>()

    override val columns: List<Column<C>> get() = _columns
    override val rows: List<Row<C>> get() = (0 until size).map {
        VirtualRow(this, it)
    }

    override fun <T : C> getValue(row: Int, column: String, type: KClass<out T>): T? {
        val value = columns[column]?.get(row)
        return type.cast(value)
    }

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

class MapColumn<T : Any, R : Any>(
    val source: Column<T>,
    override val type: KClass<out R>,
    override val name: String,
    override val meta: Meta = source.meta,
    val mapper: (T?) -> R?
) : Column<R> {
    override val size: Int get() = source.size

    override fun get(index: Int): R? = mapper(source[index])
}

class CachedMapColumn<T : Any, R : Any>(
    val source: Column<T>,
    override val type: KClass<out R>,
    override val name: String,
    override val meta: Meta = source.meta,
    val mapper: (T?) -> R?
) : Column<R> {
    override val size: Int get() = source.size
    private val values: HashMap<Int, R?> = HashMap()
    override fun get(index: Int): R? = values.getOrPut(index) { mapper(source[index]) }
}