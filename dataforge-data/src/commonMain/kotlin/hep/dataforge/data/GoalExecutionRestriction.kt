package hep.dataforge.data

import kotlin.coroutines.CoroutineContext

public enum class GoalExecutionRestrictionPolicy {
    NONE,
    WARNING,
    ERROR
}

public class GoalExecutionRestriction(
    public val policy: GoalExecutionRestrictionPolicy = GoalExecutionRestrictionPolicy.ERROR,
) : CoroutineContext.Element {
    override val key: CoroutineContext.Key<*> get() = Companion

    public companion object : CoroutineContext.Key<GoalExecutionRestriction>
}