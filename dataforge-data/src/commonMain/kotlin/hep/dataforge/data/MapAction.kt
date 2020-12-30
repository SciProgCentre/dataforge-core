package hep.dataforge.data

import hep.dataforge.meta.*
import hep.dataforge.names.Name
import kotlin.reflect.KClass

/**
 * Action environment includes data name, data meta and action configuration meta
 */
public data class ActionEnv(
    val name: Name,
    val meta: Meta,
    val actionMeta: Meta
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


public class MapAction<T : Any, out R : Any>(
    private val outputType: KClass<out R>,
    private val block: MapActionBuilder<T, R>.() -> Unit
) : Action<T, R> {

    override fun invoke(node: DataNode<T>, meta: Meta): DataNode<R> = DataTree(outputType) {
        node.dataSequence().forEach { (name, data) ->
            /*
             * Creating a new environment for action using **old** name, old meta and task meta
             */
            val env = ActionEnv(name, data.meta, meta)

            //applying transformation from builder
            val builder = MapActionBuilder<T, R>(
                name,
                data.meta.builder(), // using data meta
                meta
            ).apply(block)

            //getting new name
            val newName = builder.name

            //getting new meta
            val newMeta = builder.meta.seal()

            val newData = data.map(outputType, meta = newMeta) { builder.result(env, it) }
            //setting the data node
            this[newName] = newData
        }
    }
}

public inline fun <T : Any, reified R : Any> DataNode<T>.map(
    meta: Meta,
    noinline action: MapActionBuilder<in T, out R>.() -> Unit
): DataNode<R> = MapAction(R::class, action).invoke(this, meta)



