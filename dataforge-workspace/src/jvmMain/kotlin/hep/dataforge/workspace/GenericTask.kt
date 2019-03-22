package hep.dataforge.workspace

import hep.dataforge.data.*
import hep.dataforge.descriptors.NodeDescriptor
import hep.dataforge.meta.Meta
import hep.dataforge.meta.get
import hep.dataforge.meta.node
import hep.dataforge.names.toName
import kotlin.reflect.KClass

class GenericTask<R : Any>(
    override val name: String,
    override val type: KClass<out R>,
    override val descriptor: NodeDescriptor,
    private val modelTransform: TaskModelBuilder.(Meta) -> Unit,
    private val dataTransform: TaskModel.(DataNode<Any>) -> DataNode<R>
) : Task<R> {

    private fun gather(workspace: Workspace, model: TaskModel): DataNode<Any> {
//        val builder = DataTreeBuilder(Any::class)
//        model.dependencies.forEach { dep ->
//            dep.apply(workspace)
//        }
//        return builder.build()
    }

    override fun run(workspace: Workspace, model: TaskModel): DataNode<R> {
        //validate model
        validate(model)

        // gather data
        val input = gather(workspace, model)

        //execute
        workspace.context.logger.info("Starting task '$name' on data node ${input.name} with meta: \n${model.meta}")
        val output = dataTransform.invoke(model, input)

        //handle result
        output.handle(model.context.dispatcher) { this.handle(it) }

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
        modelTransform.invoke(builder, taskMeta)
        return builder.build()
    }
    //TODO add validation
}

class KTaskBuilder(val name: String) {
    private var modelTransform: TaskModelBuilder.(Meta) -> Unit = { data("*") }
    var descriptor: NodeDescriptor? = null

    private class DataTransformation(
        val from: String = "",
        val to: String = "",
        val transform: TaskModel.(DataNode<Any>?) -> DataNode<Any>
    ) {
        operator fun invoke(model: TaskModel, node: DataNode<Any>): DataNode<Any> {
            val localData = if (from.isEmpty()) {
                node
            } else {
                node.getNode(from.toName())
            }
            return transform.invoke(model, localData);
        }
    }

    private val dataTransforms: MutableList<DataTransformation> = ArrayList();

    fun model(modelTransform: TaskModelBuilder.(Meta) -> Unit) {
        this.modelTransform = modelTransform
    }

    fun <T : Any> transform(
        inputType: Class<T>,
        from: String = "",
        to: String = "",
        transform: TaskModel.(DataNode<T>) -> DataNode<Any>
    ) {
        dataTransforms += DataTransformation(from, to) { data: DataNode<Any> ->
            transform.invoke(this, data.checked(inputType))
        }
    }

    inline fun <reified T : Any> transform(
        from: String = "",
        to: String = "",
        noinline transform: TaskModel.(DataNode<T>) -> DataNode<Any>
    ) {
        transform(T::class.java, from, to, transform)
    }

    /**
     * Perform given action on data elements in `from` node in input and put the result to `to` node
     */
    inline fun <reified T : Any, reified R : Any> action(action: Action<T, R>, from: String = "", to: String = "") {
        transform(from, to) { data: DataNode<T> ->
            action(data, meta)
        }
    }

    inline fun <reified T : Any, reified R : Any> pipeAction(
        actionName: String = "pipe",
        from: String = "",
        to: String = "",
        noinline action: PipeBuilder<T, R>.() -> Unit
    ) {
        val pipe: Action<T, R> = PipeAction(
            inputType = T::class,
            outputType = R::class,
            block = action
        )
        action(pipe, from, to);
    }

    inline fun <reified T : Any, reified R : Any> pipe(
        actionName: String = "pipe",
        from: String = "",
        to: String = "",
        noinline action: suspend ActionEnv.(T) -> R
    ) {
        val pipe: Action<T, R> = PipeAction(
            inputType = T::class,
            outputType = R::class
        ) { result(action) }
        action(pipe, from, to);
    }


    inline fun <reified T : Any, reified R : Any> joinAction(
        actionName: String = "join",
        from: String = "",
        to: String = "",
        noinline action: JoinGroupBuilder<T, R>.() -> Unit
    ) {
        val join: Action<T, R> = JoinAction(
            inputType = T::class,
            outputType = R::class,
            action = action
        )
        action(join, from, to);
    }

    inline fun <reified T : Any, reified R : Any> join(
        actionName: String = name,
        from: String = "",
        to: String = "",
        noinline action: suspend ActionEnv.(Map<String, T>) -> R
    ) {
        val join: Action<T, R> = JoinAction(
            inputType = T::class,
            outputType = R::class,
            action = {
                result(meta.getString("@target", actionName), action)
            }
        )
        action(join, from, to);
    }

    inline fun <reified T : Any, reified R : Any> splitAction(
        actionName: String = "split",
        from: String = "",
        to: String = "",
        noinline action: SplitBuilder<T, R>.() -> Unit
    ) {
        val split: Action<T, R> = SplitAction(
            inputType = T::class,
            outputType = R::class,
            action = action
        )
        action(split, from, to);
    }

    /**
     * Use DSL to create a descriptor for this task
     */
    fun descriptor(transform: NodeDescriptor.() -> Unit) {
        this.descriptor = NodeDescriptor.build(transform)
    }

    fun build(): GenericTask<Any> {
        val transform: TaskModel.(DataNode<Any>) -> DataNode<Any> = { data ->
            if (dataTransforms.isEmpty()) {
                //return data node as is
                logger.warn("No transformation present, returning input data")
                data.checked(Any::class.java)
            } else {
                val builder = DataTreeBuilder(Any::class)
                dataTransforms.forEach {
                    val res = it(this, data)
                    if (it.to.isEmpty()) {
                        builder.update(res)
                    } else {
                        builder.putNode(it.to, res)
                    }
                }
                builder.build()
            }
        }
        return GenericTask<Any>(name, Any::class, descriptor ?: NodeDescriptor.empty(), modelTransform, transform);
    }
}

fun task(name: String, builder: KTaskBuilder.() -> Unit): GenericTask<Any> {
    return KTaskBuilder(name).apply(builder).build();
}