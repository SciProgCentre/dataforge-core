package space.kscience.dataforge.actions

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
public abstract class AbstractAction<in T : Any, R : Any>(
    public val outputType: KType,
) : Action<T, R> {

    /**
     * Generate initial content of the output
     */
    protected abstract fun DataSetBuilder<R>.generate(
        data: DataSet<T>,
        meta: Meta,
    )

    /**
     * Update part of the data set when given [updateKey] is triggered by the source
     */
    protected open fun DataSourceBuilder<R>.update(
        dataSet: DataSet<T>,
        meta: Meta,
        updateKey: Name,
    ) {
        // By default, recalculate the whole dataset
        generate(dataSet, meta)
    }

    @OptIn(DFInternal::class)
    override fun execute(
        dataSet: DataSet<T>,
        meta: Meta,
    ): DataSet<R> = if (dataSet is DataSource) {
        DataSource(outputType, dataSet){
            generate(dataSet, meta)

            launch {
                dataSet.updates.collect { name ->
                    update(dataSet, meta, name)
                }
            }
        }
    } else {
        DataTree<R>(outputType) {
            generate(dataSet, meta)
        }
    }
}
