package hep.dataforge.workspace

import hep.dataforge.context.AbstractPlugin
import hep.dataforge.names.Name
import hep.dataforge.names.toName
import hep.dataforge.workspace.old.GenericTask
import kotlin.reflect.KClass

/**
 * An abstract plugin with some additional boilerplate to effectively work with workspace context
 */
public abstract class WorkspacePlugin : AbstractPlugin() {
    private val _tasks = HashSet<WorkStage<*>>()
    public val tasks: Collection<WorkStage<*>> get() = _tasks

    override fun content(target: String): Map<Name, Any> {
        return when (target) {
            WorkStage.TYPE -> tasks.toMap()
            else -> emptyMap()
        }
    }

    public fun task(task: WorkStage<*>){
        _tasks.add(task)
    }

    public fun <T : Any> task(
        name: String,
        type: KClass<out T>,
        builder: TaskBuilder<T>.() -> Unit
    ): GenericTask<T> = TaskBuilder(name.toName(), type).apply(builder).build().also {
        _tasks.add(it)
    }

    public inline fun <reified T : Any> task(
        name: String,
        noinline builder: TaskBuilder<T>.() -> Unit
    ): GenericTask<T> = task(name, T::class, builder)

//
////TODO add delegates to build gradle-like tasks
}