package space.kscience.dataforge.actions

import space.kscience.dataforge.data.DataTree
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.misc.DFExperimental

/**
 * A simple data transformation on a data node. Actions should avoid doing actual dependency evaluation in [execute].
 */
public fun interface Action<T, R> {

    /**
     * Transform the data in the node, producing a new node. By default, it is assumed that all calculations are lazy
     * so not actual computation is started at this moment.
     */
    public fun execute(dataSet: DataTree<T>, meta: Meta): DataTree<R>

    public companion object
}

/**
 * A convenience method to transform data using given [action]
 */
public fun <T, R> DataTree<T>.transform(
    action: Action<T, R>,
    meta: Meta = Meta.EMPTY,
): DataTree<R> = action.execute(this, meta)

/**
 * Action composition. The result is terminal if one of its parts is terminal
 */
public infix fun <T, I, R> Action<T, I>.then(action: Action<I, R>): Action<T, R> = Action { dataSet, meta ->
    action.execute(this@then.execute(dataSet, meta), meta)
}

@DFExperimental
public operator fun <T, R> Action<T, R>.invoke(
    dataSet: DataTree<T>,
    meta: Meta = Meta.EMPTY,
): DataTree<R> = execute(dataSet, meta)



