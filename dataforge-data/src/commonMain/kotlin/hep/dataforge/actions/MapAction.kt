package hep.dataforge.actions

import hep.dataforge.data.*
import hep.dataforge.meta.*
import hep.dataforge.names.Name
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

/**
 * Action environment includes data name, data meta and action configuration meta
 */
public data class ActionEnv(
    val name: Name,
    val meta: Meta,
    val actionMeta: Meta,
)

/**
 * Action environment
 */
@DFBuilder
public class MapActionBuilder<T, R>(public var name: Name, public var meta: MetaBuilder, public val actionMeta: Meta) {
    public lateinit var result: suspend ActionEnv.(T) -> R

    /**
     * Calculate the result of goal
     */
    public fun result(f: suspend ActionEnv.(T) -> R) {
        result = f;
    }
}


public class MapAction<in T : Any, out R : Any>(
    public val outputType: KClass<out R>,
    private val block: MapActionBuilder<T, R>.() -> Unit,
) : Action<T, R> {

    override suspend fun execute(
        dataSet: DataSet<T>,
        meta: Meta,
        scope: CoroutineScope?,
    ): DataSet<R> {
        suspend fun mapOne(data: NamedData<T>): NamedData<R> {
            // Creating a new environment for action using **old** name, old meta and task meta
            val env = ActionEnv(data.name, data.meta, meta)

            //applying transformation from builder
            val builder = MapActionBuilder<T, R>(
                data.name,
                data.meta.toMutableMeta(), // using data meta
                meta
            ).apply(block)

            //getting new name
            val newName = builder.name

            //getting new meta
            val newMeta = builder.meta.seal()

            val newData = data.map(outputType, meta = newMeta) { builder.result(env, it) }
            //setting the data node
            return newData.named(newName)
        }

        val flow = dataSet.flow().map(::mapOne)

        return ActiveDataTree(outputType) {
            populate(flow)
            scope?.launch {
                dataSet.updates.collect { name ->
                    //clear old nodes
                    remove(name)
                    //collect new items
                    populate(dataSet.flowChildren(name).map(::mapOne))
                }
            }
        }
    }
}


@Suppress("FunctionName")
public inline fun <T : Any, reified R : Any> MapAction(
    noinline builder: MapActionBuilder<T, R>.() -> Unit,
): MapAction<T, R> = MapAction(R::class, builder)


