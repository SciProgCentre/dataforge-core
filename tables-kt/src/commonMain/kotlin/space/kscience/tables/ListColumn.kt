@file:Suppress("FunctionName")

package space.kscience.tables

import space.kscience.dataforge.meta.Meta
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * A column with data represented as [List]. Could have missing data
 */
public class ListColumn<T>(
    override val name: String,
    public val data: List<T?>,
    override val type: KType,
    override val meta: Meta,
) : Column<T> {
    override val size: Int get() = data.size

    override fun getOrNull(index: Int): T? = if (index in data.indices) data[index] else null
}

public fun <T> ListColumn(header: ColumnHeader<T>, data: List<T?>): ListColumn<T> =
    ListColumn(header.name, data, header.type, header.meta)

public inline fun <reified T> ListColumn(
    name: String,
    def: ColumnScheme,
    data: List<T?>,
): ListColumn<T> = ListColumn(name, data, typeOf<T>(), def.toMeta())

public fun <T> ListColumn(
    header: ColumnHeader<T>,
    size: Int,
    dataBuilder: (Int) -> T?,
): ListColumn<T> = ListColumn(header.name, List(size, dataBuilder), header.type, header.meta)

public inline fun <reified T> ListColumn(
    name: String,
    def: ColumnScheme,
    size: Int,
    dataBuilder: (Int) -> T?,
): ListColumn<T> = ListColumn(name, List(size, dataBuilder), typeOf<T>(), def.toMeta())

public inline fun <T, reified R : Any> Column<T>.map(meta: Meta = this.meta, noinline block: (T?) -> R): Column<R> {
    val data = List(size) { block(getOrNull(it)) }
    return ListColumn(name, data, typeOf<R>(), meta)
}