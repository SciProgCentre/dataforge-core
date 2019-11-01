package hep.dataforge.data

import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.meta.builder
import hep.dataforge.meta.seal
import hep.dataforge.names.Name
import kotlin.reflect.KClass

data class ActionEnv(val name: Name, val actionMeta: Meta, val meta: Meta)


/**
 * Action environment
 */
class MapActionBuilder<T, R>(var name: Name, var meta: MetaBuilder, val actionMeta: Meta) {
    lateinit var result: suspend ActionEnv.(T) -> R

    /**
     * Calculate the result of goal
     */
    fun result(f: suspend ActionEnv.(T) -> R) {
        result = f;
    }
}


class MapAction<T : Any, out R : Any>(
    val inputType: KClass<out T>,
    val outputType: KClass<out R>,
    private val block: MapActionBuilder<T, R>.() -> Unit
) : Action<T, R> {

    override fun invoke(node: DataNode<T>, meta: Meta): DataNode<R> {
        node.ensureType(inputType)

        return DataNode.invoke(outputType) {
            node.dataSequence().forEach { (name, data) ->
                /*
                 * Creating a new environment for action using **old** name, old meta and task meta
                 */
                val env = ActionEnv(name, meta, data.meta)

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
}

inline fun <reified T : Any, reified R : Any> DataNode<T>.map(
    meta: Meta,
    noinline action: MapActionBuilder<in T, out R>.() -> Unit
): DataNode<R> = MapAction(T::class, R::class, action).invoke(this, meta)



