package hep.dataforge.tables

import hep.dataforge.meta.Meta
import kotlin.reflect.KClass

class ListColumn<T : Any>(
    override val name: String,
    private val data: List<T?>,
    override val type: KClass<out T>,
    override val meta: Meta
) : Column<T> {
    override val size: Int get() = data.size

    override fun get(index: Int): T? = data[index]

    companion object {
        inline operator fun <reified T : Any> invoke(
            name: String,
            def: ColumnScheme,
            data: List<T?>
        ): ListColumn<T> = ListColumn(name, data, T::class, def.toMeta())

        inline operator fun <reified T : Any> invoke(
            name: String,
            def: ColumnScheme,
            size: Int,
            dataBuilder: (Int) -> T?
        ): ListColumn<T> = invoke(name, def, List(size, dataBuilder))
    }
}

inline fun <T : Any, reified R : Any> Column<T>.map(meta: Meta = this.meta, noinline block: (T?) -> R): Column<R> {
    val data = List(size) { block(get(it)) }
    return ListColumn(name, data, R::class, meta)
}