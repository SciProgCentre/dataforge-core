package space.kscience.dataforge.data

import kotlin.coroutines.CoroutineContext

public enum class GoalExecutionRestrictionPolicy {
    /**
     * Allow eager execution
     */
    NONE,

    /**
     * Give warning on eager execution
     */
    WARNING,

    /**
     * Throw error on eager execution
     */
    ERROR
}

/**
 * A special coroutine context key that allows or disallows goal execution during configuration time (eager execution).
 */
public class GoalExecutionRestriction(
    public val policy: GoalExecutionRestrictionPolicy = GoalExecutionRestrictionPolicy.ERROR,
) : CoroutineContext.Element {
    override val key: CoroutineContext.Key<*> get() = Companion

    public companion object : CoroutineContext.Key<GoalExecutionRestriction>
}