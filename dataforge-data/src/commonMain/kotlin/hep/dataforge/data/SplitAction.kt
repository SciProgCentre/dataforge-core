package hep.dataforge.data

import hep.dataforge.meta.Laminate
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.meta.builder
import hep.dataforge.names.Name
import hep.dataforge.names.toName
import kotlin.collections.set
import kotlin.reflect.KClass


public class FragmentRule<T : Any, R : Any>(public val name: Name, public var meta: MetaBuilder) {
    public lateinit var result: suspend (T) -> R

    public fun result(f: suspend (T) -> R) {
        result = f;
    }
}


public class SplitBuilder<T : Any, R : Any>(public val name: Name, public val meta: Meta) {
    internal val fragments: MutableMap<Name, FragmentRule<T, R>.() -> Unit> = HashMap()

    /**
     * Add new fragment building rule. If the framgent not defined, result won't be available even if it is present in the map
     * @param name the name of a fragment
     * @param rule the rule to transform fragment name and meta using
     */
    public fun fragment(name: String, rule: FragmentRule<T, R>.() -> Unit) {
        fragments[name.toName()] = rule
    }
}

public class SplitAction<T : Any, R : Any>(
    public val outputType: KClass<out R>,
    private val action: SplitBuilder<T, R>.() -> Unit
) : Action<T, R> {

    override fun invoke(node: DataNode<T>, meta: Meta): DataNode<R> = DataNode(outputType) {
        node.dataSequence().forEach { (name, data) ->

            val laminate = Laminate(data.meta, meta)

            val split = SplitBuilder<T, R>(name, data.meta).apply(action)


            // apply individual fragment rules to result
            split.fragments.forEach { (fragmentName, rule) ->
                val env = FragmentRule<T, R>(fragmentName, laminate.builder())

                rule(env)

                val res = data.map(outputType, meta = env.meta) { env.result(it) }
                set(env.name, res)
            }
        }
    }
}