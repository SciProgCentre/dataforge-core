package hep.dataforge.workspace

import hep.dataforge.context.Context
import hep.dataforge.context.gather
import hep.dataforge.data.DataSet
import hep.dataforge.meta.Meta
import hep.dataforge.names.Name


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