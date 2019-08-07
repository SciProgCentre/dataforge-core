package hep.dataforge.data

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Create a new [Deferred] with given [dependencies] and execution [block]. The block takes monitor as parameter.
 *
 * **Important:** Unlike regular deferred, the [Deferred] is started lazily, so the actual calculation is called only when result is requested.
 */
fun <T> goal(
    context: CoroutineContext = EmptyCoroutineContext,
    dependencies: Collection<Job> = emptyList(),
    block: suspend CoroutineScope.() -> T
): Deferred<T> = CoroutineScope(context).async(
    CoroutineMonitor() + Dependencies(dependencies),
    start = CoroutineStart.LAZY
) {
    dependencies.forEach { job ->
        job.start()
        job.invokeOnCompletion { error ->
            if (error != null) cancel(CancellationException("Dependency $job failed with error: ${error.message}"))
        }
    }
    return@async block()
}

/**
 * Create a one-to-one goal based on existing goal
 */
fun <T, R> Deferred<T>.pipe(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.(T) -> R
): Deferred<R> = goal(this + context,listOf(this)) {
    block(await())
}

/**
 * Create a joining goal.
 * @param scope the scope for resulting goal. By default use first goal in list
 */
fun <T, R> Collection<Deferred<T>>.join(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.(Collection<T>) -> R
): Deferred<R> = goal(context, this) {
    block(map { it.await() })
}

/**
 * A joining goal for a map
 * @param K type of the map key
 * @param T type of the input goal
 * @param R type of the result goal
 */
fun <K, T, R> Map<K, Deferred<T>>.join(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.(Map<K, T>) -> R
): Deferred<R> = goal(context, this.values) {
    block(mapValues { it.value.await() })
}

