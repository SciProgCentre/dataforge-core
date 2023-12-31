package space.kscience.dataforge.workspace

import kotlinx.coroutines.CoroutineScope
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.ContextBuilder
import space.kscience.dataforge.context.Global
import space.kscience.dataforge.data.*
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.MetaRepr
import space.kscience.dataforge.meta.MetaSpec
import space.kscience.dataforge.meta.MutableMeta
import space.kscience.dataforge.meta.descriptors.MetaDescriptor
import space.kscience.dataforge.meta.descriptors.MetaDescriptorBuilder
import space.kscience.dataforge.misc.DFBuilder
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.asName
import kotlin.collections.set
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty

public data class TaskReference<T : Any>(public val taskName: Name, public val task: Task<T>) : DataSelector<T> {

    @Suppress("UNCHECKED_CAST")
    override suspend fun select(workspace: Workspace, meta: Meta): DataSet<T> {
        if (workspace.tasks[taskName] == task) {
            return workspace.produce(taskName, meta) as TaskResult<T>
        } else {
            error("Task $taskName does not belong to the workspace")
        }
    }
}

public interface TaskContainer {
    /**
     * Register task in container
     */
    public fun registerTask(taskName: Name, task: Task<*>)
}

@Deprecated("use buildTask instead", ReplaceWith("buildTask(name, descriptorBuilder, builder)"))
public inline fun <reified T : Any> TaskContainer.registerTask(
    name: String,
    descriptorBuilder: MetaDescriptorBuilder.() -> Unit = {},
    noinline builder: suspend TaskResultBuilder<T>.() -> Unit,
): Unit = registerTask(Name.parse(name), Task(MetaDescriptor(descriptorBuilder), builder))

public inline fun <reified T : Any> TaskContainer.buildTask(
    name: String,
    descriptorBuilder: MetaDescriptorBuilder.() -> Unit = {},
    noinline builder: suspend TaskResultBuilder<T>.() -> Unit,
): TaskReference<T> {
    val theName = Name.parse(name)
    val descriptor = MetaDescriptor(descriptorBuilder)
    val task = Task(descriptor, builder)
    registerTask(theName, task)
    return TaskReference(theName, task)
}

public inline fun <reified T : Any> TaskContainer.task(
    descriptor: MetaDescriptor,
    noinline builder: suspend TaskResultBuilder<T>.() -> Unit,
): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, TaskReference<T>>> = PropertyDelegateProvider { _, property ->
    val taskName = Name.parse(property.name)
    val task = Task(descriptor, builder)
    registerTask(taskName, task)
    ReadOnlyProperty { _, _ -> TaskReference(taskName, task) }
}

public inline fun <reified T : Any, C : MetaRepr> TaskContainer.task(
    specification: MetaSpec<C>,
    noinline builder: suspend TaskResultBuilder<T>.(C) -> Unit,
): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, TaskReference<T>>> = PropertyDelegateProvider { _, property ->
    val taskName = Name.parse(property.name)
    val task = Task(specification, builder)
    registerTask(taskName, task)
    ReadOnlyProperty { _, _ -> TaskReference(taskName, task) }
}

public inline fun <reified T : Any> TaskContainer.task(
    noinline descriptorBuilder: MetaDescriptorBuilder.() -> Unit = {},
    noinline builder: suspend TaskResultBuilder<T>.() -> Unit,
): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, TaskReference<T>>> =
    task(MetaDescriptor(descriptorBuilder), builder)

public class WorkspaceBuilder(private val parentContext: Context = Global) : TaskContainer {
    private var context: Context? = null
    private var data: DataSet<*>? = null
    private val targets: HashMap<String, Meta> = HashMap()
    private val tasks = HashMap<Name, Task<*>>()
    private var cache: WorkspaceCache? = null

    /**
     * Define a context for the workspace
     */
    public fun context(block: ContextBuilder.() -> Unit = {}) {
        this.context = parentContext.buildContext("workspace".asName(), block)
    }

    /**
     * Define intrinsic data for the workspace
     */
    public fun data(builder: DataSetBuilder<Any>.() -> Unit) {
        data = DataTree(builder)
    }

    @DFExperimental
    public fun data(scope: CoroutineScope, builder: DataSourceBuilder<Any>.() -> Unit) {
        data = DataSource(scope, builder)
    }

    /**
     * Define a new target
     */
    public fun target(name: String, meta: Meta?) {
        if (meta == null) {
            targets.remove(name)
        } else {
            targets[name] = meta
        }
    }

    override fun registerTask(taskName: Name, task: Task<*>) {
        tasks[taskName] = task
    }

    public fun cache(cache: WorkspaceCache) {
        this.cache = cache
    }

    public fun build(): Workspace {
        val postProcess: suspend (TaskResult<*>) -> TaskResult<*> = { result ->
            cache?.evaluate(result) ?: result
        }
        return WorkspaceImpl(context ?: parentContext, data ?: DataSet.EMPTY, targets, tasks, postProcess)
    }
}

/**
 * Define a new target with a builder
 */
public inline fun WorkspaceBuilder.target(name: String, mutableMeta: MutableMeta.() -> Unit): Unit =
    target(name, Meta(mutableMeta))

@DFBuilder
public fun Workspace(parentContext: Context = Global, builder: WorkspaceBuilder.() -> Unit): Workspace =
    WorkspaceBuilder(parentContext).apply(builder).build()