package hep.dataforge.data

import hep.dataforge.meta.Laminate
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.meta.builder
import hep.dataforge.names.Name
import hep.dataforge.names.toName
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf


class JoinGroup<T : Any, R : Any>(var name: String, internal val node: DataNode<T>) {

    var meta: MetaBuilder = MetaBuilder()

    lateinit var result: suspend ActionEnv.(Map<Name, T>) -> R

    fun result(f: suspend ActionEnv.(Map<Name, T>) -> R) {
        this.result = f;
    }

}

class JoinGroupBuilder<T : Any, R : Any> {
    private val groupRules: MutableList<(DataNode<T>) -> List<JoinGroup<T, R>>> = ArrayList();

    /**
     * introduce grouping by value name
     */
    fun byValue(tag: String, defaultTag: String = "@default", action: JoinGroup<T, R>.() -> Unit) {
        groupRules += { node ->
            GroupBuilder.byValue(tag, defaultTag).invoke(node).map {
                JoinGroup<T, R>(it.key, it.value).apply(action)
            }
        }
    }

    /**
     * Add a single fixed group to grouping rules
     */
    fun group(groupName: String, filter: DataFilter, action: JoinGroup<T, R>.() -> Unit) {
        groupRules += { node ->
            listOf(
                JoinGroup<T, R>(groupName, node.filter(filter)).apply(action)
            )
        }
    }

    /**
     * Apply transformation to the whole node
     */
    fun result(resultName: String, f: suspend ActionEnv.(Map<Name, T>) -> R) {
        groupRules += { node ->
            listOf(JoinGroup<T, R>(resultName, node).apply { result(f) })
        }
    }

    internal fun buildGroups(input: DataNode<T>): List<JoinGroup<T, R>> {
        return groupRules.flatMap { it.invoke(input) }
    }

}


/**
 * The same rules as for KPipe
 */
class JoinAction<T : Any, R : Any>(
    val inputType: KClass<T>,
    val outputType: KClass<R>,
    val context: CoroutineContext = EmptyCoroutineContext,
    private val action: JoinGroupBuilder<T, R>.() -> Unit
) : Action<T, R> {

    override fun invoke(node: DataNode<T>, meta: Meta): DataNode<R> {
        if (!this.inputType.isSuperclassOf(node.type)) {
            error("$inputType expected, but ${node.type} received")
        }
        return DataNode.build(outputType) {
            JoinGroupBuilder<T, R>().apply(action).buildGroups(node).forEach { group ->

                val laminate = Laminate(group.meta, meta)

                val goalMap: Map<Name, Goal<T>> = group.node
                    .data()
                    .associate { it.first to it.second.goal }

                val groupName: String = group.name;

                val env = ActionEnv(groupName.toName(), laminate.builder())

                val goal = goalMap.join(context = context) { group.result.invoke(env, it) }

                val res = Data.of(outputType, goal, env.meta)

                set(env.name, res)
            }

        }
    }
}
