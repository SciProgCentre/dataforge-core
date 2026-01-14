package space.kscience.dataforge.actions

import kotlinx.coroutines.CoroutineScope
import space.kscience.dataforge.data.*
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
    protected abstract fun DataBuilderScope<R>.generate(
        source: DataTree<T>,
        meta: Meta,
    ): Map<Name, Data<R>>

    /**
     * Update part of the data set using provided data
     *
     * @param source the source data tree in case we need several data items to update
     * @param actionMeta the metadata used for the whole data tree
     */
    protected open suspend fun DataSink<R>.update(
        source: DataTree<T>,
        actionMeta: Meta,
        updateName: Name,
    ) {
        //by default regenerate the whole data set
        writeAll(generate(source, actionMeta))
    }

    @OptIn(UnsafeKType::class)
    override fun execute(
        source: DataTree<T>,
        meta: Meta,
        updatesScope: CoroutineScope
    ): DataTree<R> = DataTree.dynamic<R>(
        outputType,
        updatesScope,
    ) {

        generate(source, meta).forEach { (name, data) -> data(name, data) }

        update {

            //propagate updates
            val updateSink = DataSink<R> { name, data ->
                write(name, data)
            }

            with(updateSink) {
                source.updates.collect {
                    update(source, meta, it)
                }
            }
        }
    }
}

