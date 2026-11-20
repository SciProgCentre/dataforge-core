package space.kscience.tables

import space.kscience.dataforge.meta.Meta
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.typeOf

/**
 * Returns a typed view of this column with the specified Kotlin type.
 *
 * If [type] is a subtype of this column's declared type, this column is returned with
 * an unchecked generic cast. Otherwise, a wrapper column is returned that casts values
 * to the requested type when they are read.
 *
 * @param T The non-null element type of the returned column.
 * @param type The Kotlin type to expose for the returned column.
 * @return This column cast to [Column] of [T], or a wrapper column exposing the same data with the specified type.
 */
@Suppress("UNCHECKED_CAST")
public fun <T : Any> Column<*>.cast(type: KType): Column<T> {
    return if (type.isSubtypeOf(this.type)) {
        this as Column<T>
    } else {
        CastColumn(this, type)
    }
}

private class CastColumn<T : Any>(private val origin: Column<*>, override val type: KType) : Column<T> {
    override val name: String get() = origin.name
    override val meta: Meta get() = origin.meta
    override val size: Int get() = origin.size

    @Suppress("UNCHECKED_CAST")
    override fun getOrNull(index: Int): T? = origin.getOrNull(index)?.let {
        it as T
    }
}

/**
 * Returns the column whose name matches the specified [header], or `null` if no such column is present.
 *
 * The returned column is exposed with the type declared by [header].
 *
 * @param C the common non-null value type of columns in this collection.
 * @param T the non-null value type declared by the requested column header.
 * @param header the column header whose name and type are used to locate and type the column.
 * @return the matching column cast to the type declared by [header], or `null` if no column with the same name exists.
 */
public fun <C: Any, T : C> Collection<Column<C>>.getOrNull(header: ColumnHeader<T>): Column<T>? =
    find { it.name == header.name }?.cast(header.type)


/**
 * Retrieves the column matching the specified header name and exposes it with the header's type.
 *
 * The collection must contain exactly one column whose name is equal to [ColumnHeader.name].
 * The matched column is cast to the type described by [ColumnHeader.type].
 *
 * @param C The common non-null value type of columns in this collection.
 * @param T The non-null value type requested by the column header.
 * @param header The header identifying the column by name and declaring the expected value type.
 * @return The matching column cast to [Column] of [T].
 * @throws NoSuchElementException If no column with the specified header name is found.
 * @throws IllegalArgumentException If more than one column with the specified header name is found.
 */
public operator fun <C: Any, T : C> Collection<Column<C>>.get(header: ColumnHeader<T>): Column<T> =
    single { it.name == header.name }.cast(header.type)

/**
 * Returns the column with the specified name from this collection, exposed with the requested element type.
 *
 * The lookup is performed by matching [column] against each column name. If a matching column is found,
 * it is returned as a typed [Column] of [T].
 *
 * @param C The common non-null element type of columns in this collection.
 * @param T The requested non-null element type of the returned column.
 * @param column The name of the column to find.
 * @return The matching column cast to [Column] of [T], or `null` if no column with the specified name exists.
 */
public inline fun <C: Any, reified T: C> Collection<Column<C>>.getOrNull(column: String): Column<T>? =
    find { it.name == column }?.cast(typeOf<T>())

/**
 * Retrieves the single column with the specified name from this collection and returns it as a column of the requested type.
 *
 * The lookup requires exactly one column whose name matches [column]. The returned column exposes the same data cast to the
 * reified type [T].
 *
 * @param C the common non-null supertype of columns in this collection.
 * @param T the requested non-null column value type.
 * @param column the name of the column to retrieve.
 * @return the matching column cast to [Column] of [T].
 * @throws NoSuchElementException if no column with the specified name is found.
 * @throws IllegalArgumentException if more than one column with the specified name is found.
 */
public inline operator fun <C: Any, reified T: C> Collection<Column<C>>.get(column: String): Column<T> =
    single { it.name == column }.cast(typeOf<T>())