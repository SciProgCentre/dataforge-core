package space.kscience.tables

import space.kscience.dataforge.meta.Meta
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * A virtual column obtained by transforming Given row to a single value
 */
public class TransformationColumn<T, R>(
    public val table: Table<T>,
    override val type: KType,
    override val name: String,
    override val meta: Meta,
    public val mapper: (Row<T>) -> R?,
) : Column<R> {
    override val size: Int get() = table.rows.size

    override fun getOrNull(index: Int): R? = mapper(table.rows[index])
}

/**
 * A virtual column obtained via transformation of a [Row] with caching results on call (evaluation is lazy).
 *
 * Calls are not thread safe
 */
public class CachedTransformationColumn<T, R>(
    public val table: Table<T>,
    override val type: KType,
    override val name: String,
    override val meta: Meta,
    public val mapper: (Row<T>) -> R?,
) : Column<R> {
    override val size: Int get() = table.rows.size
    private val values: HashMap<Int, R?> = HashMap()
    override fun getOrNull(index: Int): R? = values.getOrPut(index) { mapper(table.rows[index]) }
}

/**
 * Create a virtual column from a given column
 */
public inline fun <T, reified R> Table<T>.rowsToColumn(
    name: String,
    meta: Meta = Meta.EMPTY,
    cache: Boolean = false,
    noinline mapper: (Row<T>) -> R?,
): Column<R> = if (cache) {
    CachedTransformationColumn(this, typeOf<R>(), name, meta, mapper)
} else {
    TransformationColumn(this, typeOf<R>(), name, meta, mapper)
}

public fun <T, R> Table<T>.rowsToColumn(
    header: ColumnHeader<R>,
    cache: Boolean = false,
    mapper: (Row<T>) -> R?,
): Column<R> = if (cache) {
    CachedTransformationColumn(this, header.type, header.name, header.meta, mapper)
} else {
    TransformationColumn(this, header.type, header.name, header.meta, mapper)
}

public fun <T> Table<T>.rowsToDoubleColumn(
    name: String,
    meta: Meta = Meta.EMPTY,
    block: (Row<T>) -> Double,
): DoubleColumn {
    val data = DoubleArray(rows.size) { block(rows[it]) }
    return DoubleColumn(name, data, meta)
}

public fun <T> Table<T>.rowsToIntColumn(
    name: String,
    meta: Meta = Meta.EMPTY,
    block: (Row<T>) -> Int,
): IntColumn {
    val data = IntArray(rows.size) { block(rows[it]) }
    return IntColumn(name, data, meta)
}