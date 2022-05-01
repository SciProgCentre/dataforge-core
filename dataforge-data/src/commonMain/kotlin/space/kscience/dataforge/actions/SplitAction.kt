package space.kscience.dataforge.actions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import space.kscience.dataforge.data.*
import space.kscience.dataforge.meta.Laminate
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.MutableMeta
import space.kscience.dataforge.meta.toMutableMeta
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.misc.DFInternal
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
    private val outputType: KType,
    private val action: SplitBuilder<T, R>.() -> Unit,
) : Action<T, R> {

    override suspend fun execute(
        dataSet: DataSet<T>,
        meta: Meta,
        scope: CoroutineScope?,
    ): DataSet<R> {

        fun splitOne(data: NamedData<T>): Sequence<NamedData<R>> {
            val laminate = Laminate(data.meta, meta)

            val split = SplitBuilder<T, R>(data.name, data.meta).apply(action)


            // apply individual fragment rules to result
            return split.fragments.entries.asSequence().map { (fragmentName, rule) ->
                val env = SplitBuilder.FragmentRule<T, R>(
                    fragmentName,
                    laminate.toMutableMeta(),
                    outputType
                ).apply(rule)
                //data.map<R>(outputType, meta = env.meta) { env.result(it) }.named(fragmentName)
                @OptIn(DFInternal::class) Data(outputType, meta = env.meta, dependencies = listOf(data)) {
                    env.result(data.await())
                }.named(fragmentName)
            }
        }

        return ActiveDataTree<R>(outputType) {
            populateWith(dataSet.dataSequence().flatMap(transform = ::splitOne))
            scope?.launch {
                dataSet.updates.collect { name ->
                    //clear old nodes
                    remove(name)
                    //collect new items
                    populateWith(dataSet.children(name).flatMap(transform = ::splitOne))
                }
            }
        }
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