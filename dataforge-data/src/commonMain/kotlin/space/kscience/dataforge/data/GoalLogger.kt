package space.kscience.dataforge.data

import kotlin.coroutines.CoroutineContext

/**
 * Coroutine context element that provides logging capabilities
 */
public interface GoalLogger : CoroutineContext.Element {
    override val key: CoroutineContext.Key<*> get() = GoalLogger

    public fun emit(vararg tags: String, message: suspend () -> String)

    public companion object : CoroutineContext.Key<GoalLogger>{
        public const val WARNING_TAG: String = "WARNING"
    }
}