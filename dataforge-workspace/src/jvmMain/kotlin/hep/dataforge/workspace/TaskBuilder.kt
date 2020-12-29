package hep.dataforge.workspace

import hep.dataforge.context.Context
import hep.dataforge.context.logger
import hep.dataforge.data.*
import hep.dataforge.meta.DFBuilder
import hep.dataforge.meta.Meta
import hep.dataforge.meta.descriptors.NodeDescriptor
import hep.dataforge.meta.get
import hep.dataforge.meta.string
import hep.dataforge.names.Name
import hep.dataforge.names.isEmpty
import hep.dataforge.names.toName
import kotlin.reflect.KClass

@DFBuilder
public class TaskBuilder<R : Any>(public val name: Name, public val type: KClass<out R>) {
    private var modelTransform: TaskModelBuilder.(Meta) -> Unit = { allData() }

    //    private val additionalDependencies = HashSet<Dependency>()
    private var descriptor: NodeDescriptor? = null
    private val dataTransforms: MutableList<DataTransformation> = ArrayList()

    /**
     * TODO will look better as extension class
     */
    private inner class DataTransformation(
        val from: String = "",
        val to: String = "",
        val transform: (Context, TaskModel, DataNode<Any>) -> DataNode<R>,
    ) {
        operator fun invoke(workspace: Workspace, model: TaskModel, node: DataNode<Any>): DataNode<R>? {
            val localData = if (from.isEmpty()) {
                node
            } else {
                node[from].node ?: return null
            }
            return transform(workspace.context, model, localData)
        }
    }

//    override fun add(dependency: Dependency) {
//        additionalDependencies.add(dependency)
//    }

    public fun model(modelTransform: TaskModelBuilder.(Meta) -> Unit) {
        this.modelTransform = modelTransform
    }

    /**
     * Add a transformation on untyped data
     */
    @JvmName("rawTransform")
    public fun transform(
        from: String = "",
        to: String = "",
        block: TaskEnv.(DataNode<*>) -> DataNode<R>,
    ) {
        dataTransforms += DataTransformation(from, to) { context, model, data ->
            val env = TaskEnv(Name.EMPTY, model.meta, context, data)
            env.block(data)
        }
    }

    public fun <T : Any> transform(
        inputType: KClass<out T>,
        from: String = "",
        to: String = "",
        block: TaskEnv.(DataNode<T>) -> DataNode<R>,
    ) {
        dataTransforms += DataTransformation(from, to) { context, model, data ->
            data.ensureType(inputType)
            val env = TaskEnv(Name.EMPTY, model.meta, context, data)
            env.block(data.cast(inputType))
        }
    }

    public inline fun <reified T : Any> transform(
        from: String = "",
        to: String = "",
        noinline block: TaskEnv.(DataNode<T>) -> DataNode<R>,
    ) {
        transform(T::class, from, to, block)
    }

    /**
     * Perform given action on data elements in `from` node in input and put the result to `to` node
     */
    public inline fun <reified T : Any> action(
        from: String = "",
        to: String = "",
        crossinline block: TaskEnv.() -> Action<T, R>,
    ) {
        transform(from, to) { data: DataNode<T> ->
            block().invoke(data, meta)
        }
    }

    public class TaskEnv(
        public val name: Name,
        public val meta: Meta,
        public val context: Context,
        public val data: DataNode<Any>,
    ) {
        public operator fun <T : Any> DirectTaskDependency<T>.invoke(): DataNode<T> = if (placement.isEmpty()) {
            data.cast(task.type)
        } else {
            data[placement].node?.cast(task.type)
                ?: error("Could not find results of direct task dependency $this at \"$placement\"")
        }
    }

    /**
     * A customized map action with ability to change meta and name
     */
    public inline fun <reified T : Any> mapAction(
        from: String = "",
        to: String = "",
        crossinline block: MapActionBuilder<T, R>.(TaskEnv) -> Unit,
    ) {
        action(from, to) {
            val env = this
            MapAction<T, R>(type) {
                block(env)
            }
        }
    }

    /**
     * A simple map action without changing meta or name
     */
    public inline fun <reified T : Any> map(
        from: String = "",
        to: String = "",
        crossinline block: suspend TaskEnv.(T) -> R,
    ) {
        action(from, to) {
            MapAction<T,R>(type) {
                //TODO automatically append task meta
                result = { data ->
                    block(data)
                }
            }
        }
    }

    /**
     * Join elements in gathered data by multiple groups
     */
    public inline fun <reified T : Any> reduceByGroup(
        from: String = "",
        to: String = "",
        crossinline block: ReduceGroupBuilder<T, R>.(TaskEnv) -> Unit,        //TODO needs KEEP-176
    ) {
        action(from, to) {
            val env = this
            ReduceAction<T, R>(type) { block(env) }
        }
    }

    /**
     * Join all elemlents in gathered data matching input type
     */
    public inline fun <reified T : Any> reduce(
        from: String = "",
        to: String = "",
        crossinline block: suspend TaskEnv.(Map<Name, T>) -> R,
    ) {
        action(from, to) {
            ReduceAction<T, R>(type) {
                result(
                    actionMeta[TaskModel.MODEL_TARGET_KEY]?.string ?: "@anonymous"
                ) { data ->
                    block(data)
                }
            }
        }
    }

    /**
     * Split each element in gathered data into fixed number of fragments
     */
    public inline fun <reified T : Any> split(
        from: String = "",
        to: String = "",
        crossinline block: SplitBuilder<T, R>.(TaskEnv) -> Unit,  //TODO needs KEEP-176
    ) {
        action(from, to) {
            val env = this
            SplitAction<T, R>(type) { block(this, env) }
        }
    }

    /**
     * Use DSL to create a descriptor for this task
     */
    public fun description(transform: NodeDescriptor.() -> Unit) {
        this.descriptor = NodeDescriptor().apply(transform)
    }

    internal fun build(): GenericTask<R> {
        return GenericTask(
            name,
            type,
            descriptor ?: NodeDescriptor(),
            modelTransform
        ) {
            val workspace = this
            return@GenericTask { data ->
                val model = this
                if (dataTransforms.isEmpty()) {
                    //return data node as is
                    logger.warn { "No transformation present, returning input data" }
                    data.ensureType(type)
                    data.cast(type)
                } else {
                    val builder = DataTreeBuilder(type)
                    dataTransforms.forEach { transformation ->
                        val res = transformation(workspace, model, data)
                        if (res != null) {
                            if (transformation.to.isEmpty()) {
                                builder.update(res)
                            } else {
                                builder[transformation.to.toName()] = res
                            }
                        }
                    }
                    builder.build()
                }
            }
        }
    }
}

