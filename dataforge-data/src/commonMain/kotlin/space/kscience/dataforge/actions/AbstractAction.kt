package space.kscience.dataforge.actions

import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import space.kscience.dataforge.data.*
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.misc.DFInternal
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
     *
     * @param source the source data tree in case we need several data items to update
     * @param meta the metadata used for the whole data tree
     * @param updatedData an updated item
     */
    protected open fun DataSink<R>.update(
        source: DataTree<T>,
        meta: Meta,
        updatedData: NamedData<T>,
    ) {
        //by default regenerate the whole data set
        generate(source, meta)
    }

    @OptIn(DFInternal::class)
    override fun execute(
        dataSet: DataTree<T>,
        meta: Meta,
    ): DataTree<R> = if (dataSet.isObservable()) {
        MutableDataTree<R>(outputType, dataSet.updatesScope).apply {
            generate(dataSet, meta)
            dataSet.updates().onEach {
                update(dataSet, meta, it)
            }.launchIn(updatesScope)

            //close updates when the source is closed
            updatesScope.launch {
                dataSet.awaitClose()
                close()
            }
        }
    } else {
        DataTree(outputType) {
            generate(dataSet, meta)
        }
    }
}
