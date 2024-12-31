package space.kscience.dataforge.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.NameToken
import space.kscience.dataforge.names.plus
import kotlin.reflect.KType

public fun interface DataFilter {

    public fun accepts(name: Name, meta: Meta?, type: KType): Boolean

    public companion object {
        public val EMPTY: DataFilter = DataFilter { _, _, _ -> true }
    }
}


//public fun DataFilter.accepts(update: DataUpdate<*>): Boolean = accepts(update.name, update.data?.meta, update.type)

//public fun <T, DU : DataUpdate<T>> Sequence<DU>.filterData(predicate: DataFilter): Sequence<DU> = filter { data ->
//    predicate.accepts(data)
//}
//
//public fun <T, DU : DataUpdate<T>> Flow<DU>.filterData(predicate: DataFilter): Flow<DU> = filter { data ->
//    predicate.accepts(data)
//}

public fun <T> DataSource<T>.filterData(
    dataFilter: DataFilter,
): DataSource<T> = object : DataSource<T> {
    override val dataType: KType get() = this@filterData.dataType

    override fun read(name: Name): Data<T>? =
        this@filterData.read(name)?.takeIf {
            dataFilter.accepts(name, it.meta, it.type)
        }
}

/**
 * Stateless filtered [ObservableDataSource]
 */
public fun <T> ObservableDataSource<T>.filterData(
    predicate: DataFilter,
): ObservableDataSource<T> = object : ObservableDataSource<T> {

    override val updates: Flow<Name>
        get() = this@filterData.updates.filter {
            val data = read(it)
            predicate.accepts(it, data?.meta, data?.type ?: dataType)
        }

    override val dataType: KType get() = this@filterData.dataType

    override fun read(name: Name): Data<T>? =
        this@filterData.read(name)?.takeIf { predicate.accepts(name, it.meta, it.type) }
}

internal class FilteredDataTree<T>(
    val source: DataTree<T>,
    val filter: DataFilter,
    val branch: Name,
    override val dataType: KType = source.dataType,
) : DataTree<T> {

    override val data: Data<T>?
        get() = source[branch].takeIf {
            filter.accepts(Name.EMPTY, it?.meta, it?.type ?: dataType)
        }

    override val items: Map<NameToken, DataTree<T>>
        get() = source.branch(branch)?.items
            ?.mapValues { FilteredDataTree(source, filter, branch + it.key) }
            ?.filter { !it.value.isEmpty() }
            ?: emptyMap()

    override val updates: Flow<Name>
        get() = source.updates.filter {
            val data = read(it)
            filter.accepts(it, data?.meta, data?.type ?: dataType)
        }
}


public fun <T> DataTree<T>.filterData(
    predicate: DataFilter,
): DataTree<T> = FilteredDataTree(this, predicate, Name.EMPTY)


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

