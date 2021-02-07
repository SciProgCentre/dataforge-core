package hep.dataforge.workspace

import hep.dataforge.data.DataSetBuilder
import hep.dataforge.data.DataTree
import hep.dataforge.data.GoalExecutionRestriction
import hep.dataforge.meta.Meta
import hep.dataforge.meta.descriptors.Described
import hep.dataforge.meta.descriptors.ItemDescriptor
import hep.dataforge.misc.DFInternal
import hep.dataforge.misc.Type
import hep.dataforge.names.Name
import hep.dataforge.workspace.Task.Companion.TYPE
import kotlinx.coroutines.withContext
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@Type(TYPE)
public interface Task<out T : Any> : Described {

    /**
     * Compute a [TaskResult] using given meta. In general, the result is lazy and represents both computation model
     * and a handler for actual result
     *
     * @param workspace a workspace to run task in
     * @param taskName the name of the task in this workspace
     * @param taskMeta configuration for current stage computation
     */
    public suspend fun execute(workspace: Workspace, taskName: Name, taskMeta: Meta): TaskResult<T>

    public companion object {
        public const val TYPE: String = "workspace.stage"
    }
}

public class TaskResultBuilder<T : Any>(
    public val workspace: Workspace,
    public val taskName: Name,
    public val taskMeta: Meta,
    private val dataDrop: DataSetBuilder<T>,
) : DataSetBuilder<T> by dataDrop

/**
 * Create a [Task] that composes a result using [builder]. Only data from the workspace could be used.
 * Data dependency cycles are not allowed.
 */
@Suppress("FunctionName")
@DFInternal
public fun <T : Any> Task(
    resultType: KType,
    descriptor: ItemDescriptor? = null,
    builder: suspend TaskResultBuilder<T>.() -> Unit,
): Task<T> = object : Task<T> {

    override val descriptor: ItemDescriptor? = descriptor

    override suspend fun execute(
        workspace: Workspace,
        taskName: Name,
        taskMeta: Meta,
    ): TaskResult<T> = withContext(GoalExecutionRestriction() + workspace.goalLogger) {
        //TODO use safe builder and check for external data on add and detects cycles
        val dataset = DataTree<T>(resultType) {
            TaskResultBuilder(workspace,taskName, taskMeta, this).apply { builder() }
        }
        workspace.internalize(dataset, taskName, taskMeta)
    }
}

@OptIn(DFInternal::class)
@Suppress("FunctionName")
public inline fun <reified T : Any> Task(
    descriptor: ItemDescriptor? = null,
    noinline builder: suspend TaskResultBuilder<T>.() -> Unit,
): Task<T> = Task(typeOf<T>(), descriptor, builder)