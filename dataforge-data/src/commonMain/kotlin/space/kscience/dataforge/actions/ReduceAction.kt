package space.kscience.dataforge.actions

import space.kscience.dataforge.data.*
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.MutableMeta
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.string
import space.kscience.dataforge.misc.DFBuilder
import space.kscience.dataforge.misc.UnsafeKType
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.parseAsName
import kotlin.reflect.KType
import kotlin.reflect.typeOf


public class JoinGroup<T, R>(
    public var name: String,
    internal val data: DataTree<T>,
    @PublishedApi internal var outputType: KType,
) {

    public var meta: MutableMeta = MutableMeta()

    public lateinit var result: suspend ActionEnv.(Map<Name, ValueWithMeta<T>>) -> R

    internal fun <R1 : R> result(outputType: KType, f: suspend ActionEnv.(Map<Name, ValueWithMeta<T>>) -> R1) {
        this.outputType = outputType
        this.result = f;
    }

    public inline fun <reified R1 : R> result(noinline f: suspend ActionEnv.(Map<Name, ValueWithMeta<T>>) -> R1) {
        outputType = typeOf<R1>()
        this.result = f;
    }

}

@DFBuilder
public class ReduceGroupBuilder<T, R>(
    public val actionMeta: Meta,
    private val outputType: KType,
) {
    private val groupRules: MutableList<(DataTree<T>) -> List<JoinGroup<T, R>>> = ArrayList();

    /**
     * Group by a meta value
     */
    @OptIn(UnsafeKType::class)
    public fun byMetaValue(tag: String, defaultTag: String = "@default", action: JoinGroup<T, R>.() -> Unit) {
        groupRules += { node ->
            val groups = mutableMapOf<String, MutableMap<Name, Data<T>>>()
            node.forEach { data ->
                groups.getOrPut(data.meta[tag]?.string ?: defaultTag) { mutableMapOf() }.put(data.name, data)
            }
            groups.map { (key, dataMap) ->
                JoinGroup<T, R>(key, dataMap.asTree(node.dataType), outputType).apply(action)
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
    public fun result(resultName: String, f: suspend ActionEnv.(Map<Name, ValueWithMeta<T>>) -> R) {
        groupRules += { node ->
            listOf(JoinGroup<T, R>(resultName, node, outputType).apply { result(outputType, f) })
        }
    }

    internal fun buildGroups(input: DataTree<T>): List<JoinGroup<T, R>> =
        groupRules.flatMap { it.invoke(input) }

}

@PublishedApi
internal class ReduceAction<T, R>(
    outputType: KType,
    private val action: ReduceGroupBuilder<T, R>.() -> Unit,
) : AbstractAction<T, R>(outputType) {
    //TODO optimize reduction. Currently, the whole action recalculates on push

    override fun DataBuilderScope<R>.generate(source: DataTree<T>, meta: Meta): Map<Name, Data<R>> = buildMap {
        ReduceGroupBuilder<T, R>(meta, outputType).apply(action).buildGroups(source).forEach { group ->
            val dataFlow: Map<Name, Data<T>> = group.data.asSequence().fold(HashMap()) { acc, value ->
                acc.apply {
                    acc[value.name] = value.data
                }
            }

            val groupName: String = group.name

            val groupMeta = group.meta

            val env = ActionEnv(groupName.parseAsName(), groupMeta, meta)
            @OptIn(UnsafeKType::class) val res: Data<R> = dataFlow.reduceToData(
                group.outputType,
                meta = groupMeta
            ) { group.result.invoke(env, it) }

            put(env.name, res)
        }
    }
}

/**
 * A one-to-one mapping action
 */
public inline fun <reified T, reified R> Action.Companion.reducing(
    noinline builder: ReduceGroupBuilder<T, R>.() -> Unit,
): Action<T, R> = ReduceAction(typeOf<R>(), builder)
