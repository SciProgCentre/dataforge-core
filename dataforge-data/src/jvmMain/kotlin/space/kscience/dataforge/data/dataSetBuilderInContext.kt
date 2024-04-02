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
public infix fun <T : Any> String.putAll(dataSet: DataTree<T>): Unit =
    putAll(this, dataSet)

/**
 * Build and append node
 */
context(DataSink<T>)
public infix fun <T : Any> String.putAll(
    block: DataSink<T>.() -> Unit,
): Unit = putAll(Name.parse(this), block)

