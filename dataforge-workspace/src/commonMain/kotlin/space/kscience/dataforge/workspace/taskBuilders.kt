package space.kscience.dataforge.workspace

import space.kscience.dataforge.data.DataSet
import space.kscience.dataforge.data.forEach
import space.kscience.dataforge.data.map
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.toMutableMeta
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.Name

/**
 * Select data using given [selector]
 */
public suspend fun <T : Any> TaskResultBuilder<*>.from(
    selector: DataSelector<T>,
): DataSet<T> = selector.select(workspace, taskMeta)

public val TaskResultBuilder<*>.allData: DataSelector<*>
    get() = object : DataSelector<Any> {
        override suspend fun select(workspace: Workspace, meta: Meta): DataSet<Any> = workspace.data
    }

/**
 * Perform a lazy mapping task using given [selector] and [action]. The meta of resulting
 * TODO move selector to receiver with multi-receivers
 */
@DFExperimental
public suspend inline fun <T : Any, reified R : Any> TaskResultBuilder<R>.pipeFrom(
    selector: DataSelector<T>,
    crossinline action: suspend (arg: T, name: Name, meta: Meta) -> R
) {
    from(selector).forEach { data ->
        val meta = data.meta.toMutableMeta().apply {
            taskName put taskMeta
        }

        val res = data.map(workspace.context.coroutineContext, meta) {
            action(it, data.name, meta)
        }

        data(data.name, res)
    }
}


