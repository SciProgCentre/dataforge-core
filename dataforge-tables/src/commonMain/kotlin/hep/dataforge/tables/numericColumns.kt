package hep.dataforge.tables

import hep.dataforge.meta.Meta
import hep.dataforge.meta.invoke
import kotlin.reflect.KClass


public class RealColumn(
    override val name: String,
    public val data: DoubleArray,
    override val meta: Meta = Meta.EMPTY
) : Column<Double> {
    override val type: KClass<out Double> get() = Double::class

    override val size: Int get() = data.size

    @Suppress("OVERRIDE_BY_INLINE", "NOTHING_TO_INLINE")
    override inline fun get(index: Int): Double = data[index]

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RealColumn) return false

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
        ): RealColumn = RealColumn(name, data, ColumnScheme(metaBuilder).toMeta())
    }
}

public class IntColumn(
    override val name: String,
    public val data: IntArray,
    override val meta: Meta = Meta.EMPTY
) : Column<Int> {
    override val type: KClass<out Int> get() = Int::class

    override val size: Int get() = data.size

    @Suppress("OVERRIDE_BY_INLINE", "NOTHING_TO_INLINE")
    override inline fun get(index: Int): Int = data[index]

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