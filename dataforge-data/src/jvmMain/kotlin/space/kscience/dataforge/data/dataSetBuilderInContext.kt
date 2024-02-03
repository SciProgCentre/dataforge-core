package space.kscience.dataforge.data

import space.kscience.dataforge.names.Name


/**
 * Append data to node
 */
context(DataSink<T>)
public infix fun <T : Any> String.put(data: Data<T>): Unit =
    put(Name.parse(this), data)

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

