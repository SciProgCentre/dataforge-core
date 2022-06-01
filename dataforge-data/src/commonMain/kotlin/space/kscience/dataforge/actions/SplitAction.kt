package space.kscience.dataforge.actions

import space.kscience.dataforge.data.*
import space.kscience.dataforge.meta.Laminate
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.MutableMeta
import space.kscience.dataforge.meta.toMutableMeta
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.Name
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
        fragments[Name.parse(name)] = rule
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

    private fun DataSetBuilder<R>.splitOne(name: Name, data: Data<T>, meta: Meta) {
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

            data(
                fragmentName,
                @Suppress("OPT_IN_USAGE") Data(outputType, meta = env.meta, dependencies = listOf(data)) {
                    env.result(data.await())
                }
            )
        }
    }

    override fun DataSetBuilder<R>.generate(data: DataSet<T>, meta: Meta) {
        data.forEach { splitOne(it.name, it.data, meta) }
    }

    override fun DataSourceBuilder<R>.update(dataSet: DataSet<T>, meta: Meta, updateKey: Name) {
        remove(updateKey)
        dataSet[updateKey]?.let { splitOne(updateKey, it, meta) }
    }
}

/**
 * Action that splits each incoming element into a number of fragments defined in builder
 */
@DFExperimental
@Suppress("FunctionName")
public inline fun <T : Any, reified R : Any> Action.Companion.split(
    noinline builder: SplitBuilder<T, R>.() -> Unit,
): Action<T, R> = SplitAction(typeOf<R>(), builder)