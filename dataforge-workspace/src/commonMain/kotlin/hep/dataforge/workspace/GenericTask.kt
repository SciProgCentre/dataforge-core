package hep.dataforge.workspace

import hep.dataforge.data.DataNode
import hep.dataforge.descriptors.NodeDescriptor
import hep.dataforge.meta.Meta
import hep.dataforge.meta.get
import hep.dataforge.meta.node
import hep.dataforge.names.Name
import kotlin.reflect.KClass

//data class TaskEnv(val workspace: Workspace, val model: TaskModel)


class GenericTask<R : Any>(
    override val name: Name,
    override val type: KClass<out R>,
    override val descriptor: NodeDescriptor,
    private val modelTransform: TaskModelBuilder.(Meta) -> Unit,
    private val dataTransform: Workspace.() -> TaskModel.(DataNode<Any>) -> DataNode<R>
) : Task<R> {

    private fun gather(workspace: Workspace, model: TaskModel): DataNode<Any> {
        return DataNode.invoke(Any::class) {
            model.dependencies.forEach { dep ->
                update(dep.apply(workspace))
            }
        }
    }

    override fun run(workspace: Workspace, model: TaskModel): DataNode<R> {
        //validate model
        validate(model)

        // gather data
        val input = gather(workspace, model)

        //execute
        workspace.context.logger.info{"Starting task '$name' on ${model.target} with meta: \n${model.meta}"}
        val output = dataTransform(workspace).invoke(model, input)

        //handle result
        //output.handle(model.context.dispatcher) { this.handle(it) }

        return output
    }

    /**
     * Build new TaskModel and apply specific model transformation for this
     * task. By default model uses the meta node with the same node as the name of the task.
     *
     * @param workspace
     * @param taskConfig
     * @return
     */
    override fun build(workspace: Workspace, taskConfig: Meta): TaskModel {
        val taskMeta = taskConfig[name]?.node ?: taskConfig
        val builder = TaskModelBuilder(name, taskMeta)
        builder.modelTransform(taskMeta)
        return builder.build()
    }
    //TODO add validation
}