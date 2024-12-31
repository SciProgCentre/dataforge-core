package space.kscience.dataforge.data

import kotlinx.coroutines.*
import space.kscience.dataforge.misc.DFExperimental
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Lazy computation result with its dependencies to allowing to stat computing dependencies ahead of time
 */
public interface Goal<out T> {
    public val dependencies: Iterable<Goal<*>>

    /**
     * Returns current running coroutine if the goal is started. Null if the computation is not started.
     */
    public val deferred: Deferred<T>?

    /**
     * Get ongoing computation or start a new one.
     * Does not guarantee thread safety. In case of multi-thread access, could create orphan computations.
     *
     * If the computation is already running, the scope is not used.
     */
    public fun async(coroutineScope: CoroutineScope): Deferred<T>

    /**
     * Reset the computation
     */
    public fun reset()

    public companion object
}

public fun Goal<*>.launchIn(coroutineScope: CoroutineScope): Job = async(coroutineScope)

public suspend fun <T> Goal<T>.await(): T = coroutineScope { async(this).await() }

public val Goal<*>.isComplete: Boolean get() = deferred?.isCompleted ?: false

public open class StaticGoal<T>(public val value: T) : Goal<T> {
    override val dependencies: Collection<Goal<*>> get() = emptyList()
    override val deferred: Deferred<T> = CompletableDeferred(value)

    override fun async(coroutineScope: CoroutineScope): Deferred<T> = deferred

    override fun reset() {
        //doNothing
    }
}

/**
 * @param coroutineContext additional context information
 */
public open class LazyGoal<T>(
    private val coroutineContext: CoroutineContext = EmptyCoroutineContext,
    override val dependencies: Iterable<Goal<*>> = emptyList(),
    public val block: suspend () -> T,
) : Goal<T> {

    final override var deferred: Deferred<T>? = null
        private set

    /**
     * Get ongoing computation or start a new one.
     * Does not guarantee thread safety. In case of multi-thread access, could create orphan computations.
     * If [GoalExecutionRestriction] is present in the [coroutineScope] context, the call could produce a error a warning
     * depending on the settings.
     */
    @OptIn(DFExperimental::class)
    override fun async(coroutineScope: CoroutineScope): Deferred<T> {
        val log = coroutineScope.coroutineContext[GoalLogger]
        // Check if context restricts goal computation
        coroutineScope.coroutineContext[GoalExecutionRestriction]?.let { restriction ->
            when (restriction.policy) {
                GoalExecutionRestrictionPolicy.WARNING -> log?.emit(GoalLogger.WARNING_TAG) { "Goal eager execution is prohibited by the coroutine scope policy" }
                GoalExecutionRestrictionPolicy.ERROR -> error("Goal eager execution is prohibited by the coroutine scope policy")
                else -> {
                    /*do nothing*/
                }
            }
        }

        log?.emit { "Starting dependencies computation for ${this@LazyGoal}" }
        val startedDependencies = dependencies.map { goal ->
            goal.async(coroutineScope)
        }
        return deferred ?: coroutineScope.async(
            coroutineContext
                    + CoroutineMonitor()
                    + Dependencies(startedDependencies)
                    + GoalExecutionRestriction(GoalExecutionRestrictionPolicy.NONE) // Remove restrictions on goal execution
        ) {
            //cancel execution if error encountered in one of dependencies
            startedDependencies.forEach { deferred ->
                deferred.invokeOnCompletion { error ->
                    if (error != null) this.cancel(CancellationException("Dependency $deferred failed with error: ${error.message}"))
                }
            }
            coroutineContext[GoalLogger]?.emit { "Starting computation of ${this@LazyGoal}" }
            block()
        }.also { deferred = it }
    }

    /**
     * Reset the computation
     */
    override fun reset() {
        deferred?.cancel()
        deferred = null
    }
}