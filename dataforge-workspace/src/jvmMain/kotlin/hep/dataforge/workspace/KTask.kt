package hep.dataforge.workspace

import hep.dataforge.data.Action
import hep.dataforge.data.DataNode
import hep.dataforge.data.DataTree
import hep.dataforge.descriptors.NodeDescriptor
import hep.dataforge.meta.Meta
import hep.dataforge.names.Name
import kotlin.reflect.KClass

class KTask<R : Any>(
    override val name: String,
    type: KClass<R>,
    descriptor: NodeDescriptor? = null,
    private val modelTransform: TaskModel.Builder.(Meta) -> Unit,
    private val dataTransform: TaskModel.(DataNode<Any>) -> DataNode<R>
) : AbstractTask<R>(type.java, descriptor) {

    override fun run(model: TaskModel, data: DataNode<Any>): DataNode<R> {
        model.context.logger.info("Starting task '$name' on data node ${data.name} with meta: \n${model.meta}")
        return dataTransform.invoke(model, data);
    }

    override fun buildModel(model: TaskModel.Builder, meta: Meta) {
        modelTransform.invoke(model, meta);
    }

    //TODO add validation
}

class KTaskBuilder(val name: String) {
    private var modelTransform: TaskModel.Builder.(Meta) -> Unit = { data("*") }
    var descriptor: NodeDescriptor? = null

    private class DataTransformation(
        val from: String = "",
        val to: String = "",
        val transform: TaskModel.(DataNode<out Any>) -> DataNode<out Any>
    ) {
        operator fun invoke(model: TaskModel, node: DataNode<Any>): DataNode<out Any> {
            val localData = if (from.isEmpty()) {
                node
            } else {
                node.getNode(from)
            }
            return transform.invoke(model, localData);
        }
    }

    private val dataTransforms: MutableList<DataTransformation> = ArrayList();

    fun model(modelTransform: TaskModel.Builder.(Meta) -> Unit) {
        this.modelTransform = modelTransform
    }

    fun <T : Any> transform(inputType: Class<T>, from: String = "", to: String = "", transform: TaskModel.(DataNode<out T>) -> DataNode<out Any>) {
        dataTransforms += DataTransformation(from, to) { data: DataNode<out Any> ->
            transform.invoke(this, data.checked(inputType))
        }
    }

    inline fun <reified T : Any> transform(from: String = "", to: String = "", noinline transform: TaskModel.(DataNode<out T>) -> DataNode<out Any>) {
        transform(T::class.java, from, to, transform)
    }

    /**
     * Perform given action on data elements in `from` node in input and put the result to `to` node
     */
    inline fun <reified T : Any, reified R : Any> action(action: Action<T, R>, from: String = "", to: String = "") {
        transform(from, to){ data: DataNode<out T> ->
            action.run(context, data, meta)
        }
    }

    inline fun <reified T : Any, reified R : Any> pipeAction(
        actionName: String = "pipe",
        from: String = "",
        to: String = "",
        noinline action: PipeBuilder<T, R>.() -> Unit) {
        val pipe: Action<T, R> = KPipe(
            actionName = Name.joinString(name, actionName),
            inputType = T::class.java,
            outputType = R::class.java,
            action = action
        )
        action(pipe, from, to);
    }

    inline fun <reified T : Any, reified R : Any> pipe(
        actionName: String = "pipe",
        from: String = "",
        to: String = "",
        noinline action: suspend ActionEnv.(T) -> R) {
        val pipe: Action<T, R> = KPipe(
            actionName = Name.joinString(name, actionName),
            inputType = T::class.java,
            outputType = R::class.java,
            action = { result(action) }
        )
        action(pipe, from, to);
    }


    inline fun <reified T : Any, reified R : Any> joinAction(
        actionName: String = "join",
        from: String = "",
        to: String = "",
        noinline action: JoinGroupBuilder<T, R>.() -> Unit) {
        val join: Action<T, R> = KJoin(
            actionName = Name.joinString(name, actionName),
            inputType = T::class.java,
            outputType = R::class.java,
            action = action
        )
        action(join, from, to);
    }

    inline fun <reified T : Any, reified R : Any> join(
        actionName: String = name,
        from: String = "",
        to: String = "",
        noinline action: suspend ActionEnv.(Map<String, T>) -> R) {
        val join: Action<T, R> = KJoin(
            actionName = Name.joinString(name, actionName),
            inputType = T::class.java,
            outputType = R::class.java,
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
        noinline action: SplitBuilder<T, R>.() -> Unit) {
        val split: Action<T, R> = KSplit(
            actionName = Name.joinString(name, actionName),
            inputType = T::class.java,
            outputType = R::class.java,
            action = action
        )
        action(split, from, to);
    }

    /**
     * Use DSL to create a descriptor for this task
     */
    fun descriptor(transform: DescriptorBuilder.() -> Unit) {
        this.descriptor = DescriptorBuilder(name).apply(transform).build()
    }

    fun build(): KTask<Any> {
        val transform: TaskModel.(DataNode<Any>) -> DataNode<Any> = { data ->
            if (dataTransforms.isEmpty()) {
                //return data node as is
                logger.warn("No transformation present, returning input data")
                data.checked(Any::class.java)
            } else {
                val builder: DataNodeEditor<Any> = DataTree.edit(Any::class.java)
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
        return KTask(name, Any::class, descriptor, modelTransform, transform);
    }
}

fun task(name: String, builder: KTaskBuilder.() -> Unit): KTask<Any> {
    return KTaskBuilder(name).apply(builder).build();
}