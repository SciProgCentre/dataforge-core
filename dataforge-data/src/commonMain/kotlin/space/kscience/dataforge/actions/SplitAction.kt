package space.kscience.dataforge.actions

import space.kscience.dataforge.data.*
import space.kscience.dataforge.meta.Laminate
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.MutableMeta
import space.kscience.dataforge.meta.toMutableMeta
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.parseAsName
import kotlin.collections.set
import kotlin.reflect.KType
import kotlin.reflect.typeOf


public class SplitBuilder<T : Any, R : Any>(public val name: Name, public val meta: Meta) {

    public class FragmentRule<T : Any, R : Any>(
        public val name: Name,
        public var meta: MutableMeta,
        @PublishedApi internal var outputType: KType,
    ) {
        public lateinit var result: suspend (T) -> R

        public inline fun <reified R1 : R> result(noinline f: suspend (T) -> R1) {
            this.outputType = typeOf<R1>()
            result = f;
        }
    }

    internal val fragments: MutableMap<Name, FragmentRule<T, R>.() -> Unit> = HashMap()

    /**
     * Add new fragment building rule. If the framgent not defined, result won't be available even if it is present in the map
     * @param name the name of a fragment
     * @param rule the rule to transform fragment name and meta using
     */
    public fun fragment(name: String, rule: FragmentRule<T, R>.() -> Unit) {
        fragments[name.parseAsName()] = rule
    }
}

/**
 * Action that splits each incoming element into a number of fragments defined in builder
 */
@PublishedApi
internal class SplitAction<T : Any, R : Any>(
    outputType: KType,
    private val action: SplitBuilder<T, R>.() -> Unit,
) : AbstractAction<T, R>(outputType) {

    private fun DataSink<R>.splitOne(name: Name, data: Data<T>, meta: Meta) {
        val laminate = Laminate(data.meta, meta)

        val split = SplitBuilder<T, R>(name, data.meta).apply(action)


        // apply individual fragment rules to result
        split.fragments.forEach { (fragmentName, rule) ->
            val env = SplitBuilder.FragmentRule<T, R>(
                fragmentName,
                laminate.toMutableMeta(),
                outputType
            ).apply(rule)
            //data.map<R>(outputType, meta = env.meta) { env.result(it) }.named(fragmentName)

            emit(
                fragmentName,
                @Suppress("OPT_IN_USAGE") Data(outputType, meta = env.meta, dependencies = listOf(data)) {
                    env.result(data.await())
                }
            )
        }
    }

    override fun DataSink<R>.generate(data: DataTree<T>, meta: Meta) {
        data.forEach { splitOne(it.name, it.data, meta) }
    }

    override fun DataSink<R>.update(allData: DataTree<T>, meta: Meta, namedData: NamedData<T>) {
        splitOne(namedData.name, namedData.data, namedData.meta)
    }
}

/**
 * Action that splits each incoming element into a number of fragments defined in builder
 */
@DFExperimental
public inline fun <T : Any, reified R : Any> Action.Companion.splitting(
    noinline builder: SplitBuilder<T, R>.() -> Unit,
): Action<T, R> = SplitAction(typeOf<R>(), builder)