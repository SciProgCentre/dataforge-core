package space.kscience.dataforge.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.plus


/**
 * Append data to node
 */
context(DataSink<T>)
public infix fun <T : Any> String.put(data: Data<T>): Unit =
    data(Name.parse(this), data)

/**
 * Append node
 */
context(DataSink<T>)
public infix fun <T : Any> String.put(dataSet: DataTree<T>): Unit =
    branch(this, dataSet)

/**
 * Build and append node
 */
context(DataSink<T>)
public infix fun <T : Any> String.put(
    block: DataSink<T>.() -> Unit,
): Unit = branch(Name.parse(this), block)

/**
 * Copy given data set and mirror its changes to this [LegacyDataTreeBuilder] in [this@setAndObserve]. Returns an update [Job]
 */
context(DataSink<T>)
public fun <T : Any> CoroutineScope.setAndWatch(
    name: Name,
    dataSet: DataTree<T>,
): Job = launch {
    branch(name, dataSet)
    dataSet.updates().collect {
        data(name + it.name, it.data)
    }
}