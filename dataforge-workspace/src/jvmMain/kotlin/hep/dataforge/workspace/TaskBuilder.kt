package hep.dataforge.workspace

import hep.dataforge.context.Context
import hep.dataforge.data.*
import hep.dataforge.meta.*
import hep.dataforge.meta.descriptors.NodeDescriptor
import hep.dataforge.names.Name
import hep.dataforge.workspace.old.GenericTask
import hep.dataforge.workspace.old.TaskModel
import hep.dataforge.workspace.old.TaskModelBuilder
import hep.dataforge.workspace.old.data
import kotlin.reflect.KClass

private typealias DataTransformation<R> = suspend (context: Context, model: TaskModel, data: DataSet<Any>) -> DataSet<R>

@DFBuilder
@DFExperimental
public class TaskBuilder<R : Any>(private val name: Name, public val type: KClass<out R>) {
    private var modelTransform: TaskModelBuilder.(Meta) -> Unit = {
        data()
    }

    //    private val additionalDependencies = HashSet<Dependency>()
    private var descriptor: NodeDescriptor? = null
    private val dataTransforms: MutableList<DataTransformation<R>> = ArrayList()

//    override fun add(dependency: Dependency) {
//        additionalDependencies.add(dependency)
//    }

    public fun model(modelTransform: TaskModelBuilder.(Meta) -> Unit) {
        this.modelTransform = modelTransform
    }


    public class TaskEnv(
        public val name: Name,
        public val meta: Meta,
        public val context: Context,
        public val data: DataSet<Any>,
    )

    /**
     * Add a transformation on untyped data
     * @param from the prefix for root node in data
     * @param to the prefix for the target node.
     */
    @JvmName("rawTransform")
    public fun transform(
        from: Name = Name.EMPTY,
        to: Name = Name.EMPTY,
        block: TaskEnv.(DataSet<*>) -> DataSet<R>,
    ) {
        dataTransforms += { context, model, data ->
            val env = TaskEnv(Name.EMPTY, model.meta, context, data)
            val startData = data.branch(from)
            env.block(startData).withNamePrefix(to)
        }
    }

    public fun <T : Any> transform(
        inputType: KClass<out T>,
        block: suspend TaskEnv.(DataSet<T>) -> DataSet<R>,
    ) {
        dataTransforms += { context, model, data ->
            val env = TaskEnv(Name.EMPTY, model.meta, context, data)
            env.block(data.filterIsInstance(inputType))
        }
    }

    public inline fun <reified T : Any> transform(
        noinline block: suspend TaskEnv.(DataSet<T>) -> DataSet<R>,
    ): Unit = transform(T::class, block)


    /**
     * Perform given action on data elements in `from` node in input and put the result to `to` node
     */
    public inline fun <reified T : Any> action(
        from: Name = Name.EMPTY,
        to: Name = Name.EMPTY,
        crossinline block: TaskEnv.() -> Action<T, R>,
    ) {
        transform { data: DataSet<T> ->
            block().execute(data, meta, context)
        }
    }


    /**
     * A customized map action with ability to change meta and name
     */
    public inline fun <reified T : Any> mapAction(
        from: Name = Name.EMPTY,
        to: Name = Name.EMPTY,
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
        from: Name = Name.EMPTY,
        to: Name = Name.EMPTY,
        crossinline block: suspend TaskEnv.(T) -> R,
    ) {
        action(from, to) {
            MapAction<T, R>(type) {
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
        from: Name = Name.EMPTY,
        to: Name = Name.EMPTY,
        crossinline block: ReduceGroupBuilder<T, R>.(TaskEnv) -> Unit,        //TODO needs KEEP-176
    ) {
        action(from, to) {
            val env = this
            ReduceAction(inputType = T::class, outputType = type) { block(env) }
        }
    }

    /**
     * Join all elemlents in gathered data matching input type
     */
    public inline fun <reified T : Any> reduce(
        from: Name = Name.EMPTY,
        to: Name = Name.EMPTY,
        crossinline block: suspend TaskEnv.(Map<Name, T>) -> R,
    ) {
        action(from, to) {
            ReduceAction(inputType = T::class, outputType = type) {
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
        from: Name = Name.EMPTY,
        to: Name = Name.EMPTY,
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
            { dataSet ->
                val model = this
                if (dataTransforms.isEmpty()) {
                    //return data node as is
                    logger.warn { "No transformation present, returning input data" }
                    dataSet.castOrNull(type) ?: error("$type expected, but $type received")
                } else {
                    DataTree.active(type, workspace.context){
                        dataTransforms.forEach { transformation ->
                            val res = transformation(workspace.context, model, dataSet)
                            update(res)
                        }
                    }
                }
            }
        }
    }
}

@DFExperimental
public suspend inline fun <reified T : Any> TaskBuilder.TaskEnv.dataTree(
    crossinline block: suspend ActiveDataTree<T>.() -> Unit,
): DataTree<T> = DataTree.active(context, block)