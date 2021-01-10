package hep.dataforge.workspace

import hep.dataforge.context.Context
import hep.dataforge.context.ContextBuilder
import hep.dataforge.context.Global
import hep.dataforge.data.DataTree
import hep.dataforge.data.MutableDataTree
import hep.dataforge.meta.*
import hep.dataforge.names.Name
import hep.dataforge.names.toName
import kotlin.reflect.KClass

@DFBuilder
public interface WorkspaceBuilder {
    public val parentContext: Context
    public var context: Context
    public var data: MutableDataTree<Any>
    public var tasks: MutableSet<Task<Any>>
    public var targets: MutableMap<String, Meta>

    public fun build(): Workspace
}

/**
 * Set the context for future workspcace
 */
public fun WorkspaceBuilder.context(name: String = "WORKSPACE", block: ContextBuilder.() -> Unit = {}) {
    context = ContextBuilder(parentContext, name).apply(block).build()
}

public inline fun <reified T : Any> WorkspaceBuilder.data(
    name: Name = Name.EMPTY,
    noinline block: MutableDataTree<T>.() -> Unit,
): DataTree<T> {
    TODO()
    //data.branch(name).apply(block)
}

@JvmName("rawData")
public fun WorkspaceBuilder.data(
    name: Name = Name.EMPTY,
    block: MutableDataTree<Any>.() -> Unit,
): DataTree<Any> = data<Any>(name, block)


public fun WorkspaceBuilder.target(name: String, block: MetaBuilder.() -> Unit) {
    targets[name] = Meta(block).seal()
}

/**
 * Use existing target as a base updating it with the block
 */
public fun WorkspaceBuilder.target(name: String, base: String, block: MetaBuilder.() -> Unit) {
    val parentTarget = targets[base] ?: error("Base target with name $base not found")
    targets[name] = parentTarget.builder()
        .apply { "@baseTarget" put base }
        .apply(block)
        .seal()
}

public fun <T : Any> WorkspaceBuilder.task(
    name: String,
    type: KClass<out T>,
    builder: TaskBuilder<T>.() -> Unit,
): Task<T> = TaskBuilder(name.toName(), type).apply(builder).build().also { tasks.add(it) }

public inline fun <reified T : Any> WorkspaceBuilder.task(
    name: String,
    noinline builder: TaskBuilder<T>.() -> Unit,
): Task<T> = task(name, T::class, builder)

@JvmName("rawTask")
public fun WorkspaceBuilder.task(
    name: String,
    builder: TaskBuilder<Any>.() -> Unit,
): Task<Any> = task(name, Any::class, builder)

/**
 * A builder for a simple workspace
 */
public class SimpleWorkspaceBuilder(override val parentContext: Context) : WorkspaceBuilder {
    override var context: Context = parentContext
    override var data: MutableDataTree<Any> = MutableDataTree(Any::class,context)
    override var tasks: MutableSet<Task<Any>> = HashSet()
    override var targets: MutableMap<String, Meta> = HashMap()

    override fun build(): SimpleWorkspace {
        return SimpleWorkspace(context, data, targets, tasks)
    }
}

public fun Workspace(
    parent: Context = Global,
    block: SimpleWorkspaceBuilder.() -> Unit,
): Workspace = SimpleWorkspaceBuilder(parent).apply(block).build()