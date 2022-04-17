package space.kscience.dataforge.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import space.kscience.dataforge.actions.Action
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

    protected abstract fun CoroutineScope.transform(
        set: DataSet<T>,
        meta: Meta,
        key: Name = Name.EMPTY,
    ): Flow<NamedData<R>>

    override suspend fun execute(
        dataSet: DataSet<T>,
        meta: Meta,
        scope: CoroutineScope?,
    ): DataSet<R> = ActiveDataTree<R>(outputType) {
        coroutineScope {
            populateWith(transform(dataSet, meta))
        }
        scope?.let {
            dataSet.updates.collect {
                //clear old nodes
                remove(it)
                //collect new items
                populateWith(scope.transform(dataSet, meta, it))
                //FIXME if the target is data, updates are fired twice
            }
        }
    }
}
