package hep.dataforge.data

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * A special deferred with explicit dependencies and some additional information like progress and unique id
 */
interface Goal<out T> : Deferred<T>, CoroutineScope {
    val scope: CoroutineScope
    override val coroutineContext get() = scope.coroutineContext

    val dependencies: Collection<Goal<*>>

    val totalWork: Double get() = dependencies.sumByDouble { totalWork } + (monitor?.totalWork ?: 0.0)
    val workDone: Double get() = dependencies.sumByDouble { workDone } + (monitor?.workDone ?: 0.0)
    val status: String get() = monitor?.status ?: ""
    val progress: Double get() = workDone / totalWork

    companion object {
        /**
         * Create goal wrapping static value. This goal is always completed
         */
        fun <T> static(scope: CoroutineScope, value: T): Goal<T> =
            StaticGoalImpl(scope, CompletableDeferred(value))
    }
}

/**
 * A monitor of goal state that could be accessed only form inside the goal
 */
class GoalMonitor : CoroutineContext.Element {
    override val key: CoroutineContext.Key<*> get() = GoalMonitor

    var totalWork: Double = 1.0
    var workDone: Double = 0.0
    var status: String = ""

    /**
     * Mark the goal as started
     */
    fun start() {

    }

    /**
     * Mark the goal as completed
     */
    fun finish() {
        workDone = totalWork
    }

    companion object : CoroutineContext.Key<GoalMonitor>
}

val CoroutineScope.monitor: GoalMonitor? get() = coroutineContext[GoalMonitor]

private class GoalImpl<T>(
    override val scope: CoroutineScope,
    override val dependencies: Collection<Goal<*>>,
    deferred: Deferred<T>
) : Goal<T>, Deferred<T> by deferred

private class StaticGoalImpl<T>(override val scope: CoroutineScope, deferred: CompletableDeferred<T>) : Goal<T>,
    Deferred<T> by deferred {
    override val dependencies: Collection<Goal<*>> get() = emptyList()
    override val status: String get() = ""
    override val totalWork: Double get() = 0.0
    override val workDone: Double get() = 0.0
}


/**
 * Create a new [Goal] with given [dependencies] and execution [block]. The block takes monitor as parameter.
 * The goal block runs in a supervised scope, meaning that when it fails, it won't affect external scope.
 *
 * **Important:** Unlike regular deferred, the [Goal] is started lazily, so the actual calculation is called only when result is requested.
 */
fun <R> CoroutineScope.createGoal(
    dependencies: Collection<Goal<*>>,
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.() -> R
): Goal<R> {
    val deferred = async(context + GoalMonitor(), start = CoroutineStart.LAZY) {
        dependencies.forEach { it.start() }
        monitor?.start()
        //Running in supervisor scope in order to allow manual error handling
        return@async supervisorScope {
            block().also {
                monitor?.finish()
            }
        }
    }

    return GoalImpl(this, dependencies, deferred)
}

/**
 * Create a one-to-one goal based on existing goal
 */
fun <T, R> Goal<T>.pipe(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.(T) -> R
): Goal<R> = createGoal(listOf(this), context) { block(await()) }

/**
 * Create a joining goal.
 * @param scope the scope for resulting goal. By default use first goal in list
 */
fun <T, R> Collection<Goal<T>>.join(
    scope: CoroutineScope = first(),
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.(Collection<T>) -> R
): Goal<R> =
    scope.createGoal(this, context) {
        block(map { it.await() })
    }