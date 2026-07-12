package space.kscience.dataforge.workspace

import space.kscience.dataforge.data.*
import space.kscience.dataforge.misc.DFInternal
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.parseAsName
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

/**
 * Select all data matching given type and filters. Does not modify paths
 *
 * @param filter additional filtering condition based on item name and meta. By default, accepts all
 */
@Suppress("UNCHECKED_CAST")
@DFInternal
public fun <R> DataTree<*>.filterByType(
    type: KType,
    filter: DataFilter = DataFilter.EMPTY,
): DataTree<R> {
    val filterWithType = DataFilter { name, meta, dataType ->
        filter.accepts(name, meta, dataType) && dataType.isSubtypeOf(type)
    }
    return FilteredDataTree(this, filterWithType, branch = Name.EMPTY, dataType = type) as DataTree<R>
}

@Suppress("UNCHECKED_CAST")
@DFInternal
public fun <R> DataTree<*>.branchByType(
    type: KType,
    branch: Name,
    filter: DataFilter = DataFilter.EMPTY,
): DataTree<R> {
    val filterWithType = DataFilter { name, meta, dataType ->
        filter.accepts(name, meta, dataType) && dataType.isSubtypeOf(type)
    }
    return FilteredDataTree(this, filterWithType, branch = branch, dataType = type) as DataTree<R>
}

/**
 * Filter data in a [DataTree] by type and optional additional [DataFilter]
 */
@OptIn(DFInternal::class)
public inline fun <reified R : Any> DataTree<*>.filterByType(
    filter: DataFilter = DataFilter.EMPTY,
): DataTree<R> = filterByType(typeOf<R>(), filter = filter)

/**
 * Filter data in a branch by type and optional additional [DataFilter]
 */
@OptIn(DFInternal::class)
public inline fun <reified R : Any> DataTree<*>.branchByType(
    branch: Name,
    filter: DataFilter = DataFilter.EMPTY,
): DataTree<R> = branchByType(typeOf<R>(), branch, filter = filter)

public inline fun <reified R : Any> DataTree<*>.branchByType(
    branch: String,
    filter: DataFilter = DataFilter.EMPTY,
): DataTree<R> = branchByType(branch.parseAsName(), filter = filter)

/**
 * Select a single datum if it is present and of given [type]
 */
public fun <R> DataTree<*>.getByType(type: KType, name: Name): NamedData<R>? =
    get(name)?.castOrNull<R>(type)?.named(name)

public inline fun <reified R : Any> DataTree<*>.getByType(name: Name): NamedData<R>? =
    this@getByType.getByType(typeOf<R>(), name)

public inline fun <reified R : Any> DataTree<*>.getByType(name: String): NamedData<R>? =
    this@getByType.getByType(typeOf<R>(), Name.parse(name))
