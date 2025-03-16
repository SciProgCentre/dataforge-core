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