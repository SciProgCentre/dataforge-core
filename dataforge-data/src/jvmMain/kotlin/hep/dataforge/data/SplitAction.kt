package hep.dataforge.data

import hep.dataforge.meta.Laminate
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.meta.builder
import hep.dataforge.names.Name
import hep.dataforge.names.toName
import kotlin.collections.set
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf


class FragmentRule<T : Any, R : Any>(val name: Name, var meta: MetaBuilder) {
    var result: suspend (T) -> R = TODO("Action not implemented")

    fun result(f: suspend (T) -> R) {
        result = f;
    }
}


class SplitBuilder<T : Any, R : Any>(val name: Name, val meta: Meta) {
    internal val fragments: MutableMap<Name, FragmentRule<T, R>.() -> Unit> = HashMap()

    /**
     * Add new fragment building rule. If the framgent not defined, result won't be available even if it is present in the map
     * @param name the name of a fragment
     * @param rule the rule to transform fragment name and meta using
     */
    fun fragment(name: String, rule: FragmentRule<T, R>.() -> Unit) {
        fragments[name.toName()] = rule
    }
}

class SplitAction<T : Any, R : Any>(
    val inputType: KClass<T>,
    val outputType: KClass<R>,
    val context: CoroutineContext = EmptyCoroutineContext,
    private val action: SplitBuilder<T, R>.() -> Unit
) : Action<T, R> {

    override fun invoke(node: DataNode<T>, meta: Meta): DataNode<R> {
        if (!this.inputType.isSuperclassOf(node.type)) {
            error("$inputType expected, but ${node.type} received")
        }

        return DataNode.build(outputType) {
            node.data().forEach { (name, data) ->

                val laminate = Laminate(data.meta, meta)

                val split = SplitBuilder<T, R>(name, data.meta).apply(action)


                // apply individual fragment rules to result
                split.fragments.forEach { fragmentName, rule ->
                    val env = FragmentRule<T, R>(fragmentName, laminate.builder())

                    rule(env)

                    val goal = data.goal.pipe(context = context) { env.result(it) }

                    val res = Data.of(outputType, goal, env.meta)
                    set(env.name, res)
                }
            }
        }
    }
}