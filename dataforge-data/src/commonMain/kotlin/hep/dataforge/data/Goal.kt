package hep.dataforge.data

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

/**
 * A special deferred with explicit dependencies and some additional information like progress and unique id
 */
interface Goal<out T> : Deferred<T>, CoroutineScope {
    val dependencies: Collection<Goal<*>>

    val status: String

    val totalWork: Double
    val workDone: Double

    val progress: Double get() = workDone / totalWork

    companion object {
        /**
         * Create goal wrapping static value. This goal is always completed
         */
        fun <T> static(context: CoroutineContext, value: T): Goal<T> =
            StaticGoalImpl(context, CompletableDeferred(value))
    }
}

/**
 * A monitor of goal state that could be accessed only form inside the goal
 */
class GoalMonitor {
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
}

private class GoalImpl<T>(
    override val dependencies: Collection<Goal<*>>,
    val monitor: GoalMonitor,
    deferred: Deferred<T>
) : Goal<T>, Deferred<T> by deferred {
    override val coroutineContext: CoroutineContext get() = this
    override val totalWork: Double get() = dependencies.sumByDouble { totalWork } + monitor.totalWork
    override val workDone: Double get() = dependencies.sumByDouble { workDone } + monitor.workDone
    override val status: String get() = monitor.status
}

private class StaticGoalImpl<T>(val context: CoroutineContext, deferred: CompletableDeferred<T>) : Goal<T>,
    Deferred<T> by deferred {
    override val dependencies: Collection<Goal<*>> get() = emptyList()
    override val status: String get() = ""
    override val totalWork: Double get() = 0.0
    override val workDone: Double get() = 0.0
    override val coroutineContext: CoroutineContext get() = context
}


/**
 * Create a new [Goal] with given [dependencies] and execution [block]. The block takes monitor as parameter.
 * The goal block runs in a supervised scope, meaning that when it fails, it won't affect external scope.
 *
 * **Important:** Unlike regular deferred, the [Goal] is started lazily, so the actual calculation is called only when result is requested.
 */
fun <R> CoroutineScope.createGoal(dependencies: Collection<Goal<*>>, block: suspend GoalMonitor.() -> R): Goal<R> {
    val monitor = GoalMonitor()
    val deferred = async(start = CoroutineStart.LAZY) {
        dependencies.forEach { it.start() }
        monitor.start()
        return@async supervisorScope { monitor.block() }
    }.also {
        monitor.finish()
    }

    return GoalImpl(dependencies, monitor, deferred)
}

/**
 * Create a one-to-one goal based on existing goal
 */
fun <T, R> Goal<T>.pipe(block: suspend GoalMonitor.(T) -> R): Goal<R> = createGoal(listOf(this)) { block(await()) }

/**
 * Create a joining goal.
 * @param scope the scope for resulting goal. By default use first goal in list
 */
fun <T, R> Collection<Goal<T>>.join(
    scope: CoroutineScope = first(),
    block: suspend GoalMonitor.(Collection<T>) -> R
): Goal<R> =
    scope.createGoal(this) {
        block(map { it.await() })
    }