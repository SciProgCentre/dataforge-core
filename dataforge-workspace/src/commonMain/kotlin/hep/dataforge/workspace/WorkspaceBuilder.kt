package hep.dataforge.workspace

import hep.dataforge.context.Context
import hep.dataforge.context.ContextBuilder
import hep.dataforge.data.Data
import hep.dataforge.data.DataNode
import hep.dataforge.data.DataTreeBuilder
import hep.dataforge.meta.*
import hep.dataforge.names.Name
import hep.dataforge.names.toName

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

fun WorkspaceBuilder.data(name: Name, data: Data<Any>) {
    this.data[name] = data
}

fun WorkspaceBuilder.data(name: String, data: Data<Any>) = data(name.toName(), data)

fun WorkspaceBuilder.static(name: Name, data: Any, meta: Meta = EmptyMeta) =
    data(name, Data.static(data, meta))

fun WorkspaceBuilder.static(name: Name, data: Any, block: MetaBuilder.() -> Unit = {}) =
    data(name, Data.static(data, buildMeta(block)))

fun WorkspaceBuilder.static(name: String, data: Any, block: MetaBuilder.() -> Unit = {}) =
    data(name, Data.static(data, buildMeta(block)))

fun WorkspaceBuilder.data(name: Name, node: DataNode<Any>) {
    this.data[name] = node
}

fun WorkspaceBuilder.data(name: String, node: DataNode<Any>) = data(name.toName(), node)

fun WorkspaceBuilder.data(name: Name, block: DataTreeBuilder<Any>.() -> Unit) {
    this.data[name] = DataNode.build(Any::class, block)
}

fun WorkspaceBuilder.data(name: String, block: DataTreeBuilder<Any>.() -> Unit) = data(name.toName(), block)

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

fun WorkspaceBuilder.task(task: Task<Any>) {
    this.tasks.add(task)
}


/**
 * A builder for a simple workspace
 */
class SimpleWorkspaceBuilder(override val parentContext: Context) : WorkspaceBuilder {
    override var context: Context = parentContext
    override var data = DataTreeBuilder(Any::class)
    override var tasks: MutableSet<Task<Any>> = HashSet()
    override var targets: MutableMap<String, Meta> = HashMap()

    override fun build(): SimpleWorkspace {
        return SimpleWorkspace(context, data.build(), targets, tasks)
    }
}