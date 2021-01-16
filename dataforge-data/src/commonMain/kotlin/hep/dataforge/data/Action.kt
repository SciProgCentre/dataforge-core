package hep.dataforge.data

import hep.dataforge.meta.Meta
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

/**
 * A simple data transformation on a data node. Actions should avoid doing actual dependency evaluation in [execute].
 */
public interface Action<in T : Any, out R : Any> {
    /**
     * Transform the data in the node, producing a new node. By default it is assumed that all calculations are lazy
     * so not actual computation is started at this moment.
     *
     * [scope] context used to compute the initial result, also it is used for updates propagation
     */
    public suspend fun execute(dataSet: DataSet<T>, meta: Meta = Meta.EMPTY, scope: CoroutineScope? = null): DataSet<R>

    public companion object
}

/**
 * Action composition. The result is terminal if one of its parts is terminal
 */
public infix fun <T : Any, I : Any, R : Any> Action<T, I>.then(action: Action<I, R>): Action<T, R> {
    // TODO introduce composite action and add optimize by adding action to the list
    return object : Action<T, R> {
        override suspend fun execute(dataSet: DataSet<T>, meta: Meta, scope: CoroutineScope?): DataSet<R> {
            return action.execute(this@then.execute(dataSet, meta, scope), meta, scope)
        }
    }
}

