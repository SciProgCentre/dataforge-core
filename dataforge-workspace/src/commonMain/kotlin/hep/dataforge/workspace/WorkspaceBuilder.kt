package hep.dataforge.workspace

import hep.dataforge.context.Context
import hep.dataforge.context.ContextBuilder
import hep.dataforge.context.Global
import hep.dataforge.data.ActiveDataTree
import hep.dataforge.data.DataSet
import hep.dataforge.data.DataSetBuilder
import hep.dataforge.data.DataTree
import hep.dataforge.meta.DFBuilder
import hep.dataforge.meta.DFExperimental
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.meta.descriptors.NodeDescriptor
import hep.dataforge.names.Name
import hep.dataforge.names.toName
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass

public data class TaskReference<T: Any>(public val taskName: Name, public val task: Task<T>)

public interface TaskContainer {
    public fun registerTask(taskName: Name, task: Task<*>)
}

public fun <T : Any> TaskContainer.registerTask(
    resultType: KClass<out T>,
    name: String,
    descriptorBuilder: NodeDescriptor.() -> Unit = {},
    builder: suspend TaskResultBuilder<T>.() -> Unit,
): Unit = registerTask(name.toName(), Task(resultType, NodeDescriptor(descriptorBuilder), builder))

public inline fun <reified T : Any> TaskContainer.registerTask(
    name: String,
    noinline descriptorBuilder: NodeDescriptor.() -> Unit = {},
    noinline builder: suspend TaskResultBuilder<T>.() -> Unit,
): Unit = registerTask(T::class, name, descriptorBuilder, builder)

public inline fun <reified T : Any> TaskContainer.task(
    noinline descriptorBuilder: NodeDescriptor.() -> Unit = {},
    noinline builder: suspend TaskResultBuilder<T>.() -> Unit,
): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, TaskReference<T>>> = PropertyDelegateProvider { _, property ->
    val taskName = property.name.toName()
    val task = Task(T::class, NodeDescriptor(descriptorBuilder), builder)
    registerTask(taskName, task)
    ReadOnlyProperty { _, _ -> TaskReference(taskName, task) }
}


public class WorkspaceBuilder(private val parentContext: Context = Global) : TaskContainer {
    private var context: Context? = null
    private var data: DataSet<*>? = null
    private val targets: HashMap<String, Meta> = HashMap()
    private val tasks = HashMap<Name, Task<*>>()

    /**
     * Define a context for the workspace
     */
    public fun context(name: String = "workspace", block: ContextBuilder.() -> Unit = {}) {
        this.context = ContextBuilder(parentContext, name).apply(block).build()
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
public inline fun WorkspaceBuilder.target(name: String, metaBuilder: MetaBuilder.() -> Unit): Unit =
    target(name, Meta(metaBuilder))

@DFBuilder
public fun Workspace(parentContext: Context = Global, builder: WorkspaceBuilder.() -> Unit): Workspace {
    return WorkspaceBuilder(parentContext).apply(builder).build()
}