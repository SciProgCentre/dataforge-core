package hep.dataforge.data

import hep.dataforge.meta.*
import hep.dataforge.names.Name
import kotlin.reflect.KClass

class ActionEnv(val name: Name, val meta: Meta)


/**
 * Action environment
 */
class MapActionBuilder<T, R>(var name: Name, var meta: MetaBuilder) {
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
                //merging data meta with action meta (data meta is primary)
                val oldMeta = meta.builder().apply { update(data.meta) }
                // creating environment from old meta and name
                val env = ActionEnv(name, oldMeta)
                //applying transformation from builder
                val builder = MapActionBuilder<T, R>(name, oldMeta).apply(block)
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



