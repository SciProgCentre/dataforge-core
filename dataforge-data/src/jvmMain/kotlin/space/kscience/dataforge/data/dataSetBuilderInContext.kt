@file:Suppress("CONTEXT_RECEIVERS_DEPRECATED")

package space.kscience.dataforge.data

import space.kscience.dataforge.names.Name


/**
 * Append data to node
 */
context(DataSink<T>)
public suspend infix fun <T : Any> String.put(data: Data<T>): Unit =
    write(Name.parse(this), data)

/**
 * Append node
 */
context(DataSink<T>)
public suspend infix fun <T : Any> String.putAll(dataSet: DataTree<T>): Unit =
    writeAll(this, dataSet)

/**
 * Build and append node
 */
context(DataSink<T>)
public infix fun <T : Any> String.putAll(
    block: DataSink<T>.() -> Unit,
): Unit = writeAll(Name.parse(this), block)

