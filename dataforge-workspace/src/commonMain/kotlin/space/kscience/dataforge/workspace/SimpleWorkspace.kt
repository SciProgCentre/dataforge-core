package space.kscience.dataforge.workspace

import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.gather
import space.kscience.dataforge.data.DataSet
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.names.Name


/**
 * A simple workspace without caching
 */
public class SimpleWorkspace(
    override val context: Context,
    data: DataSet<*>,
    override val targets: Map<String, Meta>,
    private val externalTasks: Map<Name, Task<*>>,
) : Workspace {

    override val data: TaskResult<*> = internalize(data, Name.EMPTY, Meta.EMPTY)

    override val tasks: Map<Name, Task<*>>
        get() = context.gather<Task<*>>(Task.TYPE) + externalTasks

}