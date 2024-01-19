package space.kscience.dataforge.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.names.Name
import kotlin.reflect.KType

public fun interface DataFilter {

    public fun accepts(name: Name, meta: Meta, type: KType): Boolean

    public companion object {
        public val EMPTY: DataFilter = DataFilter { _, _, _ -> true }
    }
}

public fun DataFilter.accepts(data: NamedData<*>): Boolean = accepts(data.name, data.meta, data.type)

public fun <T> Sequence<NamedData<T>>.filterData(predicate: DataFilter): Sequence<NamedData<T>> = filter { data ->
    predicate.accepts(data)
}

public fun <T> Flow<NamedData<T>>.filterData(predicate: DataFilter): Flow<NamedData<T>> = filter { data ->
    predicate.accepts(data)
}

public fun <T> DataSource<T>.filterData(
    predicate: DataFilter,
): DataSource<T> = object : DataSource<T> {
    override val dataType: KType get() = this@filterData.dataType

    override fun read(name: Name): Data<T>? =
        this@filterData.read(name)?.takeIf { predicate.accepts(name, it.meta, it.type) }
}

/**
 * Stateless filtered [ObservableDataSource]
 */
public fun <T> ObservableDataSource<T>.filterData(
    predicate: DataFilter,
): ObservableDataSource<T> = object : ObservableDataSource<T> {
    override fun updates(): Flow<NamedData<T>> = this@filterData.updates().filter { predicate.accepts(it) }

    override val dataType: KType get() = this@filterData.dataType

    override fun read(name: Name): Data<T>? =
        this@filterData.read(name)?.takeIf { predicate.accepts(name, it.meta, it.type) }
}

public fun <T> GenericDataTree<T, *>.filterData(
    predicate: DataFilter,
): DataTree<T> = asSequence().filterData(predicate).toTree(dataType)

public fun <T> GenericObservableDataTree<T, *>.filterData(
    scope: CoroutineScope,
    predicate: DataFilter,
): ObservableDataTree<T> = asSequence().filterData(predicate).toObservableTree(dataType, scope, updates().filterData(predicate))


///**
// * Generate a wrapper data set with a given name prefix appended to all names
// */
//public fun <T : Any> DataTree<T>.withNamePrefix(prefix: Name): DataSet<T> = if (prefix.isEmpty()) {
//    this
//} else object : DataSource<T> {
//
//    override val dataType: KType get() = this@withNamePrefix.dataType
//
//    override val coroutineContext: CoroutineContext
//        get() = (this@withNamePrefix as? DataSource)?.coroutineContext ?: EmptyCoroutineContext
//
//    override val meta: Meta get() = this@withNamePrefix.meta
//
//
//    override fun iterator(): Iterator<NamedData<T>> = iterator {
//        for (d in this@withNamePrefix) {
//            yield(d.data.named(prefix + d.name))
//        }
//    }
//
//    override fun get(name: Name): Data<T>? =
//        name.removeFirstOrNull(name)?.let { this@withNamePrefix.get(it) }
//
//    override val updates: Flow<Name> get() = this@withNamePrefix.updates.map { prefix + it }
//}
//

