package space.kscience.dataforge.actions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import space.kscience.dataforge.data.DataSink
import space.kscience.dataforge.data.DataTree
import space.kscience.dataforge.data.DataUpdate
import space.kscience.dataforge.data.launchUpdate
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.misc.UnsafeKType
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
public abstract class AbstractAction<T, R>(
    public val outputType: KType,
) : Action<T, R> {

    /**
     * Generate initial content of the output
     */
    protected abstract fun DataSink<R>.generate(
        source: DataTree<T>,
        meta: Meta,
    )

    /**
     * Update part of the data set using provided data
     *
     * @param source the source data tree in case we need several data items to update
     * @param meta the metadata used for the whole data tree
     * @param updatedData an updated item
     */
    protected open suspend fun DataSink<R>.update(
        source: DataTree<T>,
        meta: Meta,
        updatedData: DataUpdate<T>,
    ) {
        //by default regenerate the whole data set
        generate(source, meta)
    }

    @OptIn(UnsafeKType::class)
    override fun execute(
        source: DataTree<T>,
        meta: Meta,
        updatesScope: CoroutineScope
    ): DataTree<R> = DataTree(outputType) {
        generate(source, meta)
        //propagate updates
        launchUpdate(updatesScope) {
            source.updates.onEach { update ->
                update(source, meta, update)
            }.collect()
        }
    }
}

