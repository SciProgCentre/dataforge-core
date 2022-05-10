package space.kscience.dataforge.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.plus


/**
 * Append data to node
 */
context(DataSetBuilder<T>) public infix fun <T : Any> String.put(data: Data<T>): Unit =
    data(Name.parse(this), data)

/**
 * Append node
 */
context(DataSetBuilder<T>) public infix fun <T : Any> String.put(dataSet: DataSet<T>): Unit =
    node(Name.parse(this), dataSet)

/**
 * Build and append node
 */
context(DataSetBuilder<T>) public infix fun <T : Any> String.put(
    block: DataSetBuilder<T>.() -> Unit,
): Unit = node(Name.parse(this), block)

/**
 * Copy given data set and mirror its changes to this [DataTreeBuilder] in [this@setAndObserve]. Returns an update [Job]
 */
context(DataSetBuilder<T>) public fun <T : Any> CoroutineScope.setAndWatch(
    name: Name,
    dataSet: DataSet<T>,
): Job = launch {
    node(name, dataSet)
    dataSet.updates.collect { nameInBranch ->
        data(name + nameInBranch, dataSet.get(nameInBranch))
    }
}