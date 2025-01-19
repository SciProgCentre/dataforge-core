package space.kscience.dataforge.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.misc.DFInternal
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

/**
 * A [DataTree] filtered by branch and some criterion, possibly changing resulting type
 */
@DFInternal
public class FilteredDataTree<T>(
    public val source: DataTree<T>,
    public val filter: DataFilter,
    public val branch: Name,
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
): FilteredDataTree<T> = FilteredDataTree(this, predicate, Name.EMPTY)