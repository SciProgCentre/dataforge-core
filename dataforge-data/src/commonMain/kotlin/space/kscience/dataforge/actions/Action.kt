package space.kscience.dataforge.actions

import space.kscience.dataforge.data.DataSet
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.misc.DFExperimental

/**
 * A simple data transformation on a data node. Actions should avoid doing actual dependency evaluation in [execute].
 */
public fun interface Action<in T : Any, out R : Any> {

    /**
     * Transform the data in the node, producing a new node. By default, it is assumed that all calculations are lazy
     * so not actual computation is started at this moment.
     */
    public fun execute(dataSet: DataSet<T>, meta: Meta): DataSet<R>

    public companion object
}

/**
 * A convenience method to transform data using given [action]
 */
public fun <T : Any, R : Any> DataSet<T>.transform(action: Action<T, R>, meta: Meta = Meta.EMPTY): DataSet<R> =
    action.execute(this, meta)

/**
 * Action composition. The result is terminal if one of its parts is terminal
 */
public infix fun <T : Any, I : Any, R : Any> Action<T, I>.then(action: Action<I, R>): Action<T, R> =
    Action<T, R> { dataSet, meta -> action.execute(this@then.execute(dataSet, meta), meta) }

@DFExperimental
public operator fun <T : Any, R : Any> Action<T, R>.invoke(
    dataSet: DataSet<T>,
    meta: Meta = Meta.EMPTY,
): DataSet<R> = execute(dataSet, meta)

