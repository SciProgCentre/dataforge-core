package hep.dataforge.workspace

import hep.dataforge.context.Context
import hep.dataforge.context.ContextBuilder
import hep.dataforge.data.DataTreeBuilder
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.meta.buildMeta

/**
 * A builder for a workspace
 */
class WorkspaceBuilder(var context: Context) {
    val data = DataTreeBuilder<Any>()
    val targets = HashMap<String, Meta>()
    val tasks = HashSet<Task<Any>>()

    fun context(action: ContextBuilder.() -> Unit) {
        this.context = ContextBuilder().apply(action).build()
    }

    fun data(action: DataTreeBuilder<Any>.() -> Unit) = data.apply(action)

    fun target(name: String, meta: Meta) {
        targets[name] = meta
    }

    fun target(name: String, action: MetaBuilder.() -> Unit) = target(name, buildMeta(action))

    fun task(task: Task<*>) {
        tasks.add(task)
    }

    fun build(): Workspace = SimpleWorkspace(
        context,
        data.build(),
        targets,
        tasks
    )
}