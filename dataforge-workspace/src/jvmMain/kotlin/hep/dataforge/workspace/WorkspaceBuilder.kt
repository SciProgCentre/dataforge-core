package hep.dataforge.workspace

import hep.dataforge.context.Context
import hep.dataforge.context.ContextBuilder
import hep.dataforge.context.Global
import hep.dataforge.data.ActiveDataTree
import hep.dataforge.meta.*
import hep.dataforge.names.toName
import kotlin.reflect.KClass

@DFBuilder
public interface WorkspaceBuilder {
    public val parentContext: Context
    public var context: Context
    public var data: ActiveDataTree<Any>
    public var tasks: MutableSet<WorkStage<Any>>
    public var targets: MutableMap<String, Meta>

    public fun build(): Workspace
}

/**
 * Set the context for future workspcace
 */
public fun WorkspaceBuilder.context(name: String = "WORKSPACE", block: ContextBuilder.() -> Unit = {}) {
    context = ContextBuilder(parentContext, name).apply(block).build()
}

public inline fun WorkspaceBuilder.data(
    block: ActiveDataTree<Any>.() -> Unit,
): Unit{
    data.apply(block)
}


public fun WorkspaceBuilder.target(name: String, block: MetaBuilder.() -> Unit) {
    targets[name] = Meta(block).seal()
}

class WorkspaceTask(val workspace: Workspace, val name: String)

/**
 * Use existing target as a base updating it with the block
 */
public fun WorkspaceBuilder.target(name: String, base: String, block: MetaBuilder.() -> Unit) {
    val parentTarget = targets[base] ?: error("Base target with name $base not found")
    targets[name] = parentTarget.toMutableMeta()
        .apply { "@baseTarget" put base }
        .apply(block)
        .seal()
}

public fun <T : Any> WorkspaceBuilder.task(
    name: String,
    type: KClass<out T>,
    builder: TaskBuilder<T>.() -> Unit,
): WorkspaceTask = TaskBuilder(name.toName(), type).apply(builder).build().also { tasks.add(it) }

public inline fun <reified T : Any> WorkspaceBuilder.task(
    name: String,
    noinline builder: TaskBuilder<T>.() -> Unit,
): WorkStage<T> = task(name, T::class, builder)

@JvmName("rawTask")
public fun WorkspaceBuilder.task(
    name: String,
    builder: TaskBuilder<Any>.() -> Unit,
): WorkStage<Any> = task(name, Any::class, builder)

/**
 * A builder for a simple workspace
 */
public class SimpleWorkspaceBuilder(override val parentContext: Context) : WorkspaceBuilder {
    override var context: Context = parentContext
    override var data: ActiveDataTree<Any> = ActiveDataTree(Any::class, context)
    override var tasks: MutableSet<WorkStage<Any>> = HashSet()
    override var targets: MutableMap<String, Meta> = HashMap()

    override fun build(): SimpleWorkspace {
        return SimpleWorkspace(context, data, targets, tasks)
    }
}

public fun Workspace(
    parent: Context = Global,
    block: SimpleWorkspaceBuilder.() -> Unit,
): Workspace = SimpleWorkspaceBuilder(parent).apply(block).build()