package hep.dataforge.workspace

import hep.dataforge.data.DataSet
import hep.dataforge.meta.Meta
import hep.dataforge.meta.descriptors.Described
import hep.dataforge.misc.Type
import hep.dataforge.workspace.Task.Companion.TYPE
import kotlin.reflect.KClass

@Type(TYPE)
public interface Task<out R : Any> : Described {

    /**
     * The explicit type of the node returned by the task
     */
    public val type: KClass<out R>

    /**
     * Build a model for this task. Does not run any computations unless task [isEager]
     *
     * @param workspace
     * @param taskMeta
     * @return
     */
    public fun build(workspace: Workspace, taskMeta: Meta): TaskModel

    /**
     * Check if the model is valid and is acceptable by the task. Throw exception if not.
     *
     * @param model
     */
    public fun validate(model: TaskModel) {
        if(this.name != model.name) error("The task $name could not be run with model from task ${model.name}")
    }

    /**
     * Run given task model. Type check expected to be performed before actual
     * calculation.
     *
     * @param workspace - a workspace to run task model in
     * @param model - a model to be executed
     * @return
     */
    public suspend fun run(workspace: Workspace, model: TaskModel): DataSet<R>

    public companion object {
        public const val TYPE: String = "task"
    }
}