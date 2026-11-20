package space.kscience.tables

import space.kscience.dataforge.meta.Meta
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Optimized primitive-holding column
 */
public class DoubleColumn(
    override val name: String,
    public val data: DoubleArray,
    override val meta: Meta = Meta.EMPTY
) : Column<Double> {
    override val type: KType get() = typeOf<Double>()

    override val size: Int get() = data.size

    override fun getOrNull(index: Int): Double = data[index]

    /**
     * Performance-optimized get method
     */
    public fun getDouble(index: Int): Double = data[index]

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DoubleColumn) return false

        if (name != other.name) return false
        if (!data.contentEquals(other.data)) return false
        if (meta != other.meta) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + meta.hashCode()
        return result
    }

    public companion object {
        public inline operator fun <reified T : Any> invoke(
            name: String,
            data: DoubleArray,
            noinline metaBuilder: ColumnScheme.() -> Unit
        ): DoubleColumn = DoubleColumn(name, data, ColumnScheme(metaBuilder).toMeta())
    }
}

/**
 * A column of non-null integer values backed by an [IntArray].
 *
 * Values are stored in primitive form to avoid boxing in the backing storage. For valid
 * indices, every position contains an integer value and access does not return `null`.
 * Valid indices are in the range from `0` inclusive to [size] exclusive.
 *
 * Equality and hash code are based on the column name, the contents of the backing
 * array, and the associated metadata.
 *
 * @param name the column name exposed by its header
 * @param data the backing array containing column values
 * @param meta metadata associated with the column
 */
public class IntColumn(
    override val name: String,
    public val data: IntArray,
    override val meta: Meta = Meta.EMPTY
) : Column<Int> {
    override val type: KType get() = typeOf<Int>()

    override val size: Int get() = data.size

    override fun getOrNull(index: Int): Int = data[index]

    /**
     * Performance optimized get method
     */
    public fun getInt(index: Int): Int = data[index]

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IntColumn) return false

        if (name != other.name) return false
        if (!data.contentEquals(other.data)) return false
        if (meta != other.meta) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + meta.hashCode()
        return result
    }

    public companion object {
        public inline operator fun <reified T : Any> invoke(
            name: String,
            data: IntArray,
            noinline metaBuilder: ColumnScheme.() -> Unit
        ): IntColumn = IntColumn(name, data, ColumnScheme(metaBuilder).toMeta())
    }
}


/**
 * Create a virtual column from a given column header and sequential data
 */
public fun <T> Table<T>.rowsToDoubleColumn(
    name: String,
    meta: Meta = Meta.EMPTY,
    block: (Row<T>) -> Double,
): DoubleColumn {
    val data = DoubleArray(rows.size) { block(rows[it]) }
    return DoubleColumn(name, data, meta)
}

/**
 * Create a virtual column from a given column header and sequential data
 */
public fun <T> Table<T>.rowsToIntColumn(
    name: String,
    meta: Meta = Meta.EMPTY,
    block: (Row<T>) -> Int,
): IntColumn {
    val data = IntArray(rows.size) { block(rows[it]) }
    return IntColumn(name, data, meta)
}