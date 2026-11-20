package space.kscience.dataforge.actions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
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
    public fun execute(source: DataTree<T>, meta: Meta, updatesScope: CoroutineScope): DataTree<R>

    public companion object
}

/**
 * A convenience method to transform data using given [action]
 */
@OptIn(DelicateCoroutinesApi::class)
public fun <T, R> DataTree<T>.transform(
    action: Action<T, R>,
    meta: Meta = Meta.EMPTY,
    updateScope: CoroutineScope = GlobalScope,
): DataTree<R> = action.execute(this, meta, updateScope)

/**
 * Action composition. The result is terminal if one of its parts is terminal
 */
public infix fun <T, I, R> Action<T, I>.then(action: Action<I, R>): Action<T, R> = Action { dataSet, meta, scope ->
    action.execute(this@then.execute(dataSet, meta, scope), meta, scope)
}

/**
 * Invokes this action on the given data tree and returns the transformed data tree.
 *
 * This is a convenience operator for [Action.execute]. The provided [meta] is passed as
 * action configuration, and [updateScope] is used for observing and propagating updates
 * from the source data tree.
 *
 * @param dataSet the source data tree to transform.
 * @param meta the action configuration metadata.
 * @param updateScope the coroutine scope used to handle update propagation.
 * @return the resulting data tree produced by this action.
 */
@DFExperimental
@OptIn(DelicateCoroutinesApi::class)
public operator fun <T, R> Action<T, R>.invoke(
    dataSet: DataTree<T>,
    meta: Meta = Meta.EMPTY,
    updateScope: CoroutineScope = GlobalScope,
): DataTree<R> = execute(dataSet, meta, updateScope)



