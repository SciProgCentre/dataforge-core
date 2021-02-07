package hep.dataforge.actions

import hep.dataforge.data.*
import hep.dataforge.meta.Laminate
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.meta.toMutableMeta
import hep.dataforge.misc.DFExperimental
import hep.dataforge.misc.DFInternal
import hep.dataforge.names.Name
import hep.dataforge.names.toName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.collections.set
import kotlin.reflect.KType
import kotlin.reflect.typeOf


public class SplitBuilder<T : Any, R : Any>(public val name: Name, public val meta: Meta) {

    public class FragmentRule<T : Any, R : Any>(public val name: Name, public var meta: MetaBuilder) {
        public lateinit var result: suspend (T) -> R

        public fun result(f: suspend (T) -> R) {
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
        fragments[name.toName()] = rule
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

    @OptIn(FlowPreview::class)
    override suspend fun execute(
        dataSet: DataSet<T>,
        meta: Meta,
        scope: CoroutineScope?,
    ): DataSet<R> {

        suspend fun splitOne(data: NamedData<T>): Flow<NamedData<R>> {
            val laminate = Laminate(data.meta, meta)

            val split = SplitBuilder<T, R>(data.name, data.meta).apply(action)


            // apply individual fragment rules to result
            return split.fragments.entries.asFlow().map { (fragmentName, rule) ->
                val env = SplitBuilder.FragmentRule<T, R>(fragmentName, laminate.toMutableMeta()).apply(rule)
                //data.map<R>(outputType, meta = env.meta) { env.result(it) }.named(fragmentName)
                @OptIn(DFInternal::class) Data(outputType, meta = env.meta, dependencies = listOf(data)) {
                    env.result(data.await())
                }.named(fragmentName)
            }
        }

        return ActiveDataTree<R>(outputType) {
            populate(dataSet.flow().flatMapConcat(transform = ::splitOne))
            scope?.launch {
                dataSet.updates.collect { name ->
                    //clear old nodes
                    remove(name)
                    //collect new items
                    populate(dataSet.flowChildren(name).flatMapConcat(transform = ::splitOne))
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