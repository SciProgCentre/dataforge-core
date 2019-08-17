package hep.dataforge.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

/**
 * A monitor of goal state that could be accessed only form inside the goal
 */
class CoroutineMonitor : CoroutineContext.Element {
    override val key: CoroutineContext.Key<*> get() = CoroutineMonitor

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

    companion object : CoroutineContext.Key<CoroutineMonitor>
}

class Dependencies(val values: Collection<Job>) : CoroutineContext.Element {
    override val key: CoroutineContext.Key<*> get() = Dependencies

    companion object : CoroutineContext.Key<Dependencies>
}

val CoroutineContext.monitor: CoroutineMonitor? get() = this[CoroutineMonitor]
val CoroutineScope.monitor: CoroutineMonitor? get() = coroutineContext.monitor

val Job.dependencies: Collection<Job> get() = this[Dependencies]?.values ?: emptyList()

val Job.totalWork: Double get() = dependencies.sumByDouble { totalWork } + (monitor?.totalWork ?: 0.0)
val Job.workDone: Double get() = dependencies.sumByDouble { workDone } + (monitor?.workDone ?: 0.0)
val Job.status: String get() = monitor?.status ?: ""
val Job.progress: Double get() = workDone / totalWork