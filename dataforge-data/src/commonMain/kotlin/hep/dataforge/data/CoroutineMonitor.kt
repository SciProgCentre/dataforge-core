package hep.dataforge.data

import hep.dataforge.meta.DFExperimental
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext



/**
 * A monitor of goal state that could be accessed only form inside the goal
 */
@DFExperimental
public class CoroutineMonitor : CoroutineContext.Element {
    override val key: CoroutineContext.Key<*> get() = CoroutineMonitor

    public var totalWork: Double = 1.0
    public var workDone: Double = 0.0
    public var status: String = ""

    /**
     * Mark the goal as started
     */
    public fun start() {

    }

    /**
     * Mark the goal as completed
     */
    public fun finish() {
        workDone = totalWork
    }

    public companion object : CoroutineContext.Key<CoroutineMonitor>
}

public class Dependencies(public val values: Collection<Job>) : CoroutineContext.Element {
    override val key: CoroutineContext.Key<*> get() = Dependencies

    public companion object : CoroutineContext.Key<Dependencies>
}

@DFExperimental
public val CoroutineContext.monitor: CoroutineMonitor? get() = this[CoroutineMonitor]
@DFExperimental
public val CoroutineScope.monitor: CoroutineMonitor? get() = coroutineContext.monitor

public val Job.dependencies: Collection<Job> get() = this[Dependencies]?.values ?: emptyList()

@DFExperimental
public val Job.totalWork: Double get() = dependencies.sumByDouble { totalWork } + (monitor?.totalWork ?: 0.0)
@DFExperimental
public val Job.workDone: Double get() = dependencies.sumByDouble { workDone } + (monitor?.workDone ?: 0.0)
@DFExperimental
public val Job.status: String get() = monitor?.status ?: ""
@DFExperimental
public val Job.progress: Double get() = workDone / totalWork