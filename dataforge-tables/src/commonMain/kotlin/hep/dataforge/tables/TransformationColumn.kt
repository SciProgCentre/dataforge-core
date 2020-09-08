package hep.dataforge.tables

import hep.dataforge.meta.Meta
import kotlin.reflect.KClass

/**
 * A virtual column obtained by transforming Given row to a single value
 */
public class TransformationColumn<T : Any, R : Any>(
    public val table: Table<T>,
    override val type: KClass<out R>,
    override val name: String,
    override val meta: Meta,
    public val mapper: (Row<T>) -> R?
) : Column<R> {
    override val size: Int get() = table.rows.size

    override fun get(index: Int): R? = mapper(table.rows[index])
}

/**
 * A virtual column obtained via transformation of single column with caching results on call (evaluation is lazy).
 *
 * Calls are not thread safe
 */
public class CachedTransformationColumn<T : Any, R : Any>(
    public val table: Table<T>,
    override val type: KClass<out R>,
    override val name: String,
    override val meta: Meta,
    public val mapper: (Row<T>) -> R?
) : Column<R> {
    override val size: Int get() = table.rows.size
    private val values: HashMap<Int, R?> = HashMap()
    override fun get(index: Int): R? = values.getOrPut(index) { mapper(table.rows[index]) }
}

/**
 * Create a virtual column from a given column
 */
public inline fun <T : Any, reified R : Any> Table<T>.mapRows(
    name: String,
    meta: Meta = Meta.EMPTY,
    cache: Boolean = false,
    noinline mapper: (Row<T>) -> R?
): Column<R> = if (cache) {
    CachedTransformationColumn(this, R::class, name, meta, mapper)
} else {
    TransformationColumn(this, R::class, name, meta, mapper)
}

public fun <T : Any> Table<T>.mapRowsToDouble(name: String, meta: Meta = Meta.EMPTY, block: (Row<T>) -> Double): RealColumn {
    val data = DoubleArray(rows.size) { block(rows[it]) }
    return RealColumn(name, data, meta)
}

public fun <T : Any> Table<T>.mapRowsToInt(name: String, meta: Meta = Meta.EMPTY, block: (Row<T>) -> Int): IntColumn {
    val data = IntArray(rows.size) { block(rows[it]) }
    return IntColumn(name, data, meta)
}