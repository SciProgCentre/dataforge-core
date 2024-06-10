package space.kscience.dataforge.workspace

import kotlinx.coroutines.withContext
import space.kscience.dataforge.data.DataSink
import space.kscience.dataforge.data.GoalExecutionRestriction
import space.kscience.dataforge.data.MutableDataTree
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.MetaReader
import space.kscience.dataforge.meta.MetaRepr
import space.kscience.dataforge.meta.descriptors.Described
import space.kscience.dataforge.meta.descriptors.MetaDescriptor
import space.kscience.dataforge.misc.DfType
import space.kscience.dataforge.misc.UnsafeKType
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.workspace.Task.Companion.TYPE
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * A configurable task that could be executed on a workspace. The [TaskResult] represents a lazy result of the task.
 * In general no computations should be made until the result is called.
 */
@DfType(TYPE)
public interface Task<T> : Described {

    /**
     * A task identification string used to compare tasks and check task body for change
     */
    public val fingerprint: String get() = hashCode().toString(radix = 16)

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
        public const val TYPE: String = "workspace.task"
    }
}

/**
 * A [Task] with [MetaReader] for wrapping and unwrapping task configuration
 */
public interface TaskWithSpec<T, C : Any> : Task<T> {
    public val spec: MetaReader<C>
    override val descriptor: MetaDescriptor? get() = spec.descriptor

    public suspend fun execute(workspace: Workspace, taskName: Name, configuration: C): TaskResult<T>

    override suspend fun execute(workspace: Workspace, taskName: Name, taskMeta: Meta): TaskResult<T> =
        execute(workspace, taskName, spec.read(taskMeta))
}

//public suspend fun <T : Any, C : Scheme> TaskWithSpec<T, C>.execute(
//    workspace: Workspace,
//    taskName: Name,
//    block: C.() -> Unit = {},
//): TaskResult<T> = execute(workspace, taskName, spec(block))

public class TaskResultBuilder<T>(
    public val workspace: Workspace,
    public val taskName: Name,
    public val taskMeta: Meta,
    private val dataSink: DataSink<T>,
) : DataSink<T> by dataSink

/**
 * Create a [Task] that composes a result using [builder]. Only data from the workspace could be used.
 * Data dependency cycles are not allowed.
 *
 * @param resultType the type boundary for data produced by this task
 * @param descriptor of meta accepted by this task
 * @param builder for resulting data set
 */
public fun <T : Any> Task(
    resultType: KType,
    descriptor: MetaDescriptor? = null,
    builder: suspend TaskResultBuilder<T>.() -> Unit,
): Task<T> = object : Task<T> {

    override val descriptor: MetaDescriptor? = descriptor

    override suspend fun execute(
        workspace: Workspace,
        taskName: Name,
        taskMeta: Meta,
    ): TaskResult<T> {
        //TODO use safe builder and check for external data on add and detects cycles
        @OptIn(UnsafeKType::class)
        val dataset = MutableDataTree<T>(resultType).apply {
            TaskResultBuilder(workspace, taskName, taskMeta, this).apply {
                withContext(GoalExecutionRestriction() + workspace.goalLogger) {
                    builder()
                }
            }
        }
        return workspace.wrapResult(dataset, taskName, taskMeta)
    }
}

public inline fun <reified T : Any> Task(
    descriptor: MetaDescriptor? = null,
    noinline builder: suspend TaskResultBuilder<T>.() -> Unit,
): Task<T> = Task(typeOf<T>(), descriptor, builder)


/**
 * Create a [Task] that composes a result using [builder]. Only data from the workspace could be used.
 * Data dependency cycles are not allowed.
 *
 * @param resultType the type boundary for data produced by this task
 * @param specification a specification for task configuration
 * @param builder for resulting data set
 */


@Suppress("FunctionName")
public fun <T : Any, C : MetaRepr> Task(
    resultType: KType,
    specification: MetaReader<C>,
    builder: suspend TaskResultBuilder<T>.(C) -> Unit,
): TaskWithSpec<T, C> = object : TaskWithSpec<T, C> {
    override val spec: MetaReader<C> = specification

    override suspend fun execute(
        workspace: Workspace,
        taskName: Name,
        configuration: C,
    ): TaskResult<T> = withContext(GoalExecutionRestriction() + workspace.goalLogger) {
        //TODO use safe builder and check for external data on add and detects cycles
        val taskMeta = configuration.toMeta()
        @OptIn(UnsafeKType::class)
        val dataset = MutableDataTree<T>(resultType).apply {
            TaskResultBuilder(workspace, taskName, taskMeta, this).apply { builder(configuration) }
        }
        workspace.wrapResult(dataset, taskName, taskMeta)
    }
}

public inline fun <reified T : Any, C : MetaRepr> Task(
    specification: MetaReader<C>,
    noinline builder: suspend TaskResultBuilder<T>.(C) -> Unit,
): Task<T> = Task(typeOf<T>(), specification, builder)