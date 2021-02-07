package hep.dataforge.actions

import hep.dataforge.data.*
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.meta.seal
import hep.dataforge.meta.toMutableMeta
import hep.dataforge.misc.DFBuilder
import hep.dataforge.misc.DFExperimental
import hep.dataforge.misc.DFInternal
import hep.dataforge.names.Name
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.reflect.KType
import kotlin.reflect.typeOf

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

@PublishedApi
internal class MapAction<in T : Any, out R : Any>(
    private val outputType: KType,
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

            @OptIn(DFInternal::class) val newData = Data(outputType, newMeta, dependencies = listOf(data)) {
                builder.result(env, data.await())
            }
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


/**
 * A one-to-one mapping action
 */
@DFExperimental
@Suppress("FunctionName")
public inline fun <T : Any, reified R : Any> Action.Companion.map(
    noinline builder: MapActionBuilder<T, R>.() -> Unit,
): Action<T, R> = MapAction(typeOf<R>(), builder)


