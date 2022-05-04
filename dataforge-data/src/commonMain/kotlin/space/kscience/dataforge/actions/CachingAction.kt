package space.kscience.dataforge.actions

import kotlinx.coroutines.launch
import space.kscience.dataforge.data.*
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.startsWith
import kotlin.reflect.KType

/**
 * Remove all values with keys starting with [name]
 */
internal fun MutableMap<Name, *>.removeWhatStartsWith(name: Name) {
    val toRemove = keys.filter { it.startsWith(name) }
    toRemove.forEach(::remove)
}

/**
 * An action that caches results on-demand and recalculates them on source push
 */
public abstract class CachingAction<in T : Any, out R : Any>(
    public val outputType: KType,
) : Action<T, R> {

    protected abstract fun transform(
        set: DataSet<T>,
        meta: Meta,
        key: Name = Name.EMPTY,
    ): Sequence<NamedData<R>>

    override fun execute(
        dataSet: DataSet<T>,
        meta: Meta,
    ): DataSet<R> = if (dataSet is DataSource) {
        DataSourceBuilder<R>(outputType, dataSet.coroutineContext).apply {
            populateFrom(transform(dataSet, meta))

            launch {
                dataSet.updates.collect {
                    //clear old nodes
                    remove(it)
                    //collect new items
                    populateFrom(transform(dataSet, meta, it))
                    //FIXME if the target is data, updates are fired twice
                }
            }
        }
    } else {
        DataTree<R>(outputType) {
            populateFrom(transform(dataSet, meta))
        }
    }
}
