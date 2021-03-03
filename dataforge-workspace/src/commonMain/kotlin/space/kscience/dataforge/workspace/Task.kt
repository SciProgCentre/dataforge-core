package space.kscience.dataforge.workspace

import kotlinx.coroutines.withContext
import space.kscience.dataforge.data.DataSetBuilder
import space.kscience.dataforge.data.DataTree
import space.kscience.dataforge.data.GoalExecutionRestriction
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.descriptors.Described
import space.kscience.dataforge.meta.descriptors.ItemDescriptor
import space.kscience.dataforge.misc.DFInternal
import space.kscience.dataforge.misc.Type
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.workspace.Task.Companion.TYPE
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