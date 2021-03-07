package space.kscience.dataforge.workspace

import kotlinx.coroutines.launch
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.info
import space.kscience.dataforge.context.logger
import space.kscience.dataforge.data.GoalLogger

public class ContextGoalLogger(public val context: Context) : GoalLogger {
    override fun emit(vararg tags: String, message: suspend () -> String) {
        context.launch {
            val text = message()
            context.logger.info { text }
        }
    }
}

public val Workspace.goalLogger: GoalLogger get() = ContextGoalLogger(context)