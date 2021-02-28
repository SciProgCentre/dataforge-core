package hep.dataforge.workspace

import hep.dataforge.context.Context
import hep.dataforge.context.info
import hep.dataforge.context.logger
import hep.dataforge.data.GoalLogger
import kotlinx.coroutines.launch

public class ContextGoalLogger(public val context: Context) : GoalLogger {
    override fun emit(vararg tags: String, message: suspend () -> String) {
        context.launch {
            val text = message()
            context.logger.info { text }
        }
    }
}

public val Workspace.goalLogger: GoalLogger get() = ContextGoalLogger(context)