package space.kscience.dataforge.workspace

import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.ContextBuilder
import space.kscience.dataforge.context.Global
import space.kscience.dataforge.data.ActiveDataTree
import space.kscience.dataforge.data.DataSet
import space.kscience.dataforge.data.DataSetBuilder
import space.kscience.dataforge.data.DataTree
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.MetaRepr
import space.kscience.dataforge.meta.MutableMeta
import space.kscience.dataforge.meta.Specification
import space.kscience.dataforge.meta.descriptors.MetaDescriptor
import space.kscience.dataforge.meta.descriptors.MetaDescriptorBuilder
import space.kscience.dataforge.misc.DFBuilder
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.asName
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
    specification: Specification<C>,
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

    /**
     * Define a context for the workspace
     */
    public fun context(block: ContextBuilder.() -> Unit = {}) {
        this.context = parentContext.buildContext("workspace".asName(), block)
    }

    /**
     * Define intrinsic data for the workspace
     */
    public suspend fun buildData(builder: suspend DataSetBuilder<Any>.() -> Unit) {
        data = DataTree(builder)
    }

    @DFExperimental
    public suspend fun buildActiveData(builder: suspend ActiveDataTree<Any>.() -> Unit) {
        data = ActiveDataTree(builder)
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

    public fun build(): Workspace = SimpleWorkspace(context ?: parentContext, data ?: DataSet.EMPTY, targets, tasks)
}

/**
 * Define a new target with a builder
 */
public inline fun WorkspaceBuilder.target(name: String, mutableMeta: MutableMeta.() -> Unit): Unit =
    target(name, Meta(mutableMeta))

@DFBuilder
public fun Workspace(parentContext: Context = Global, builder: WorkspaceBuilder.() -> Unit): Workspace =
    WorkspaceBuilder(parentContext).apply(builder).build()