package hep.dataforge.data

import kotlin.coroutines.CoroutineContext

public interface GoalLogger : CoroutineContext.Element {
    override val key: CoroutineContext.Key<*> get() = GoalLogger

    public fun emit(vararg tags: String, message: suspend () -> String)

    public companion object : CoroutineContext.Key<GoalLogger>{
        public const val WARNING_TAG: String = "WARNING"
    }
}