package space.kscience.dataforge.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import space.kscience.dataforge.misc.DFInternal
import space.kscience.dataforge.names.Name
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.typeOf


/**
 * Cast the node to given type if the cast is possible or return null
 */
@Suppress("UNCHECKED_CAST")
private fun <R> Data<*>.castOrNull(type: KType): Data<R>? =
    if (!this.type.isSubtypeOf(type)) {
        null
    } else {
        object : Data<R> by (this as Data<R>) {
            override val type: KType = type
        }
    }

@Suppress("UNCHECKED_CAST")
@DFInternal
public fun <R> Sequence<DataUpdate<*>>.filterByDataType(type: KType): Sequence<NamedData<R>> =
    filter { it.type.isSubtypeOf(type) } as Sequence<NamedData<R>>

@Suppress("UNCHECKED_CAST")
@DFInternal
public fun <R> Flow<DataUpdate<*>>.filterByDataType(type: KType): Flow<NamedData<R>> =
    filter { it.type.isSubtypeOf(type) } as Flow<NamedData<R>>

/**
 * Select all data matching given type and filters. Does not modify paths
 *
 * @param filter additional filtering condition based on item name and meta. By default, accepts all
 */
@Suppress("UNCHECKED_CAST")
@DFInternal
public fun <R> DataTree<*>.filterByType(
    type: KType,
    branch: Name = Name.EMPTY,
    filter: DataFilter = DataFilter.EMPTY,
): DataTree<R> {
    val filterWithType = DataFilter { name, meta, dataType ->
        filter.accepts(name, meta, dataType) && dataType.isSubtypeOf(type)
    }
    return FilteredDataTree(this, filterWithType, branch, type) as DataTree<R>
}

/**
 * Select a single datum of the appropriate type
 */
@OptIn(DFInternal::class)
public inline fun <reified R : Any> DataTree<*>.filterByType(
    branch: Name = Name.EMPTY,
    filter: DataFilter = DataFilter.EMPTY,
): DataTree<R> = filterByType(typeOf<R>(), branch, filter = filter)

/**
 * Select a single datum if it is present and of given [type]
 */
public fun <R> DataTree<*>.getByType(type: KType, name: Name): NamedData<R>? =
    get(name)?.castOrNull<R>(type)?.named(name)

public inline fun <reified R : Any> DataTree<*>.getByType(name: Name): NamedData<R>? =
    this@getByType.getByType(typeOf<R>(), name)

public inline fun <reified R : Any> DataTree<*>.getByType(name: String): NamedData<R>? =
    this@getByType.getByType(typeOf<R>(), Name.parse(name))
