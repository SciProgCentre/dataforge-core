@file:Suppress("CONTEXT_RECEIVERS_DEPRECATED")

package space.kscience.dataforge.data

import space.kscience.dataforge.names.Name


/**
 * Append data to node
 */
context(sink: DataSink<T>)
public suspend infix fun <T : Any> String.put(data: Data<T>): Unit =
    sink.write(Name.parse(this), data)

/**
 * Append node
 */
context(sink: DataSink<T>)
public suspend infix fun <T : Any> String.putAll(dataSet: DataTree<T>): Unit =
    sink.writeAll(this, dataSet)

/**
 * Build and append node
 */
context(sink: DataSink<T>)
public infix fun <T : Any> String.putAll(
    block: DataSink<T>.() -> Unit,
): Unit = sink.writeAll(Name.parse(this), block)

