package space.kscience.dataforge.actions

import space.kscience.dataforge.data.DataSet
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.misc.DFExperimental

/**
 * A simple data transformation on a data node. Actions should avoid doing actual dependency evaluation in [execute].
 */
public interface Action<in T : Any, out R : Any> {

    /**
     * Transform the data in the node, producing a new node. By default, it is assumed that all calculations are lazy
     * so not actual computation is started at this moment.
     */
    public fun execute(dataSet: DataSet<T>, meta: Meta = Meta.EMPTY): DataSet<R>

    public companion object
}

/**
 * Action composition. The result is terminal if one of its parts is terminal
 */
public infix fun <T : Any, I : Any, R : Any> Action<T, I>.then(action: Action<I, R>): Action<T, R> {
    // TODO introduce composite action and add optimize by adding action to the list
    return object : Action<T, R> {

        override fun execute(
            dataSet: DataSet<T>,
            meta: Meta,
        ): DataSet<R> = action.execute(this@then.execute(dataSet, meta), meta)
    }
}

@DFExperimental
public operator fun <T : Any, R : Any> Action<T, R>.invoke(
    dataSet: DataSet<T>,
    meta: Meta = Meta.EMPTY,
): DataSet<R> = execute(dataSet, meta)

