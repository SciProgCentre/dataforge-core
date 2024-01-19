package space.kscience.dataforge.actions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import space.kscience.dataforge.data.DataTree
import space.kscience.dataforge.data.ObservableDataTree
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.misc.DFExperimental

/**
 * A simple data transformation on a data node. Actions should avoid doing actual dependency evaluation in [execute].
 */
public fun interface Action<T : Any, R : Any> {

    /**
     * Transform the data in the node, producing a new node. By default, it is assumed that all calculations are lazy
     * so not actual computation is started at this moment.
     */
    public fun execute(scope: CoroutineScope, dataSet: DataTree<T>, meta: Meta): ObservableDataTree<R>

    public companion object
}

/**
 * A convenience method to transform data using given [action]
 */
public fun <T : Any, R : Any> DataTree<T>.transform(
    action: Action<T, R>,
    scope: CoroutineScope,
    meta: Meta = Meta.EMPTY,
): DataTree<R> = action.execute(scope, this, meta)

/**
 * Action composition. The result is terminal if one of its parts is terminal
 */
public infix fun <T : Any, I : Any, R : Any> Action<T, I>.then(action: Action<I, R>): Action<T, R> =
    Action { scope, dataSet, meta -> action.execute(scope, this@then.execute(scope, dataSet, meta), meta) }

@DFExperimental
public suspend operator fun <T : Any, R : Any> Action<T, R>.invoke(
    dataSet: DataTree<T>,
    meta: Meta = Meta.EMPTY,
): DataTree<R> = coroutineScope { execute(this, dataSet, meta) }


