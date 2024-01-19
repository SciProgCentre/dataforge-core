package space.kscience.dataforge.actions

import space.kscience.dataforge.data.*
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.MutableMeta
import space.kscience.dataforge.misc.DFBuilder
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.misc.DFInternal
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.parseAsName
import kotlin.reflect.KType
import kotlin.reflect.typeOf


public class JoinGroup<T : Any, R : Any>(
    public var name: String,
    internal val set: DataTree<T>,
    @PublishedApi internal var outputType: KType,
) {

    public var meta: MutableMeta = MutableMeta()

    public lateinit var result: suspend ActionEnv.(Map<Name, ValueWithMeta<T>>) -> R

    internal fun <R1 : R> result(outputType: KType, f: suspend ActionEnv.(Map<Name,  ValueWithMeta<T>>) -> R1) {
        this.outputType = outputType
        this.result = f;
    }

    public inline fun <reified R1 : R> result(noinline f: suspend ActionEnv.(Map<Name,  ValueWithMeta<T>>) -> R1) {
        outputType = typeOf<R1>()
        this.result = f;
    }

}

@DFBuilder
public class ReduceGroupBuilder<T : Any, R : Any>(
    public val actionMeta: Meta,
    private val outputType: KType,
) {
    private val groupRules: MutableList<(DataTree<T>) -> List<JoinGroup<T, R>>> = ArrayList();

    /**
     * introduce grouping by meta value
     */
    public fun byValue(tag: String, defaultTag: String = "@default", action: JoinGroup<T, R>.() -> Unit) {
        groupRules += { node ->
            GroupRule.byMetaValue(tag, defaultTag).gather(node).map {
                JoinGroup<T, R>(it.key, it.value, outputType).apply(action)
            }
        }
    }

    public fun group(
        groupName: String,
        predicate: DataFilter,
        action: JoinGroup<T, R>.() -> Unit,
    ) {
        groupRules += { source ->
            listOf(
                JoinGroup<T, R>(groupName, source.filterData(predicate), outputType).apply(action)
            )
        }
    }

    /**
     * Apply transformation to the whole node
     */
    public fun result(resultName: String, f: suspend ActionEnv.(Map<Name,  ValueWithMeta<T>>) -> R) {
        groupRules += { node ->
            listOf(JoinGroup<T, R>(resultName, node, outputType).apply { result(outputType, f) })
        }
    }

    internal fun buildGroups(input: DataTree<T>): List<JoinGroup<T, R>> =
        groupRules.flatMap { it.invoke(input) }

}

@PublishedApi
internal class ReduceAction<T : Any, R : Any>(
    outputType: KType,
    private val action: ReduceGroupBuilder<T, R>.() -> Unit,
) : AbstractAction<T, R>(outputType) {
    //TODO optimize reduction. Currently, the whole action recalculates on push

    override fun DataSink<R>.generate(data: DataTree<T>, meta: Meta) {
        ReduceGroupBuilder<T, R>(meta, outputType).apply(action).buildGroups(data).forEach { group ->
            val dataFlow: Map<Name, Data<T>> = group.set.asSequence().fold(HashMap()) { acc, value ->
                acc.apply {
                    acc[value.name] = value.data
                }
            }

            val groupName: String = group.name

            val groupMeta = group.meta

            val env = ActionEnv(groupName.parseAsName(), groupMeta, meta)
            @OptIn(DFInternal::class) val res: Data<R> = dataFlow.reduceToData(
                group.outputType,
                meta = groupMeta
            ) { group.result.invoke(env, it) }

            emit(env.name, res)
        }
    }
}

/**
 * A one-to-one mapping action
 */
@DFExperimental
public inline fun <reified T : Any, reified R : Any> Action.Companion.reducing(
    noinline builder: ReduceGroupBuilder<T, R>.() -> Unit,
): Action<T, R> = ReduceAction(typeOf<R>(), builder)
