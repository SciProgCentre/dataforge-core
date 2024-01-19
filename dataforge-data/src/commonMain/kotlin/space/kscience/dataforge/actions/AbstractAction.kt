package space.kscience.dataforge.actions

import kotlinx.coroutines.CoroutineScope
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
public abstract class AbstractAction<T : Any, R : Any>(
    public val outputType: KType,
) : Action<T, R> {

    /**
     * Generate initial content of the output
     */
    protected abstract fun DataSink<R>.generate(
        data: DataTree<T>,
        meta: Meta,
    )

    /**
     * Update part of the data set using provided data
     */
    protected open fun DataSink<R>.update(
        allData: DataTree<T>,
        meta: Meta,
        namedData: NamedData<T>,
    ){
        //by default regenerate the whole data set
        generate(allData,meta)
    }

    override fun execute(
        scope: CoroutineScope,
        dataSet: DataTree<T>,
        meta: Meta,
    ): ObservableDataTree<R> = MutableDataTree<R>(outputType, scope).apply {
        generate(dataSet, meta)
        scope.launch {
            dataSet.updates().collect {
                update(dataSet, meta, it)
            }
        }
    }
}
