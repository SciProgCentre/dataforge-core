package hep.dataforge.workspace

import hep.dataforge.context.Context
import hep.dataforge.context.ContextBuilder
import hep.dataforge.data.DataNode
import hep.dataforge.data.DataTreeBuilder
import hep.dataforge.meta.*
import hep.dataforge.names.EmptyName
import hep.dataforge.names.Name
import hep.dataforge.names.isEmpty
import hep.dataforge.names.toName
import kotlin.jvm.JvmName
import kotlin.reflect.KClass

@TaskBuildScope
interface WorkspaceBuilder {
    val parentContext: Context
    var context: Context
    var data: DataTreeBuilder<Any>
    var tasks: MutableSet<Task<Any>>
    var targets: MutableMap<String, Meta>

    fun build(): Workspace
}


/**
 * Set the context for future workspcace
 */
fun WorkspaceBuilder.context(name: String = "WORKSPACE", block: ContextBuilder.() -> Unit = {}) {
    context = ContextBuilder(name, parentContext).apply(block).build()
}

inline fun <reified T : Any> WorkspaceBuilder.data(
    name: Name = EmptyName,
    noinline block: DataTreeBuilder<T>.() -> Unit
): DataNode<T> {
    val node = DataTreeBuilder(T::class).apply(block)
    if (name.isEmpty()) {
        @Suppress("UNCHECKED_CAST")
        data = node as DataTreeBuilder<Any>
    } else {
        data[name] = node
    }
    return node.build()
}

@JvmName("rawData")
fun WorkspaceBuilder.data(
    name: Name = EmptyName,
    block: DataTreeBuilder<Any>.() -> Unit
): DataNode<Any> = data<Any>(name, block)


fun WorkspaceBuilder.target(name: String, block: MetaBuilder.() -> Unit) {
    targets[name] = buildMeta(block).seal()
}

/**
 * Use existing target as a base updating it with the block
 */
fun WorkspaceBuilder.target(name: String, base: String, block: MetaBuilder.() -> Unit) {
    val parentTarget = targets[base] ?: error("Base target with name $base not found")
    targets[name] = parentTarget.builder()
        .apply { "@baseTarget" to base }
        .apply(block)
        .seal()
}

fun <T : Any> WorkspaceBuilder.task(
    name: String,
    type: KClass<out T>,
    builder: TaskBuilder<T>.() -> Unit
): Task<T> = TaskBuilder(name.toName(), type).apply(builder).build().also { tasks.add(it) }

inline fun <reified T : Any> WorkspaceBuilder.task(
    name: String,
    noinline builder: TaskBuilder<T>.() -> Unit
): Task<T> = task(name, T::class, builder)

@JvmName("rawTask")
fun WorkspaceBuilder.task(
    name: String,
    builder: TaskBuilder<Any>.() -> Unit
): Task<Any> = task(name, Any::class, builder)

/**
 * A builder for a simple workspace
 */
class SimpleWorkspaceBuilder(override val parentContext: Context) : WorkspaceBuilder {
    override var context: Context = parentContext
    override var data: DataTreeBuilder<Any> = DataTreeBuilder(Any::class)
    override var tasks: MutableSet<Task<Any>> = HashSet()
    override var targets: MutableMap<String, Meta> = HashMap()

    override fun build(): SimpleWorkspace {
        return SimpleWorkspace(context, data.build(), targets, tasks)
    }
}