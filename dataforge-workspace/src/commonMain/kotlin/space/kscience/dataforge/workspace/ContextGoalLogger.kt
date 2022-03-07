package space.kscience.dataforge.workspace

import kotlinx.coroutines.launch
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.info
import space.kscience.dataforge.context.logger
import space.kscience.dataforge.data.GoalLogger

/**
 * A coroutine context key that injects a [Context] bound logger into the scope.
 * The message body is computed asynchronously
 */
public class ContextGoalLogger(public val context: Context) : GoalLogger {
    override fun emit(vararg tags: String, message: suspend () -> String) {
        context.launch {
            val text = message()
            context.logger.info { text }
        }
    }
}

public val Workspace.goalLogger: GoalLogger get() = ContextGoalLogger(context)