package hep.dataforge.data

import kotlinx.coroutines.runBlocking

/**
 * Block the thread and get data content
 */
fun <T : Any> Data<T>.get(): T = runBlocking { task.await() }