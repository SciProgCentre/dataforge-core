package hep.dataforge.data

import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.names.Name
import hep.dataforge.names.toName
import kotlin.reflect.KClass


class JoinGroup<T : Any, R : Any>(var name: String, internal val node: DataNode<T>) {

    var meta: MetaBuilder = MetaBuilder()

    lateinit var result: suspend ActionEnv.(Map<Name, T>) -> R

    fun result(f: suspend ActionEnv.(Map<Name, T>) -> R) {
        this.result = f;
    }

}

class ReduceGroupBuilder<T : Any, R : Any>(val actionMeta: Meta) {
    private val groupRules: MutableList<(DataNode<T>) -> List<JoinGroup<T, R>>> = ArrayList();

    /**
     * introduce grouping by value name
     */
    fun byValue(tag: String, defaultTag: String = "@default", action: JoinGroup<T, R>.() -> Unit) {
        groupRules += { node ->
            GroupRule.byValue(tag, defaultTag).invoke(node).map {
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

    fun group(groupName: String, filter: (Name, Data<T>) -> Boolean, action: JoinGroup<T, R>.() -> Unit) {
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
class ReduceAction<T : Any, R : Any>(
    val inputType: KClass<out T>,
    val outputType: KClass<out R>,
    private val action: ReduceGroupBuilder<T, R>.() -> Unit
) : Action<T, R> {

    override fun invoke(node: DataNode<T>, meta: Meta): DataNode<R> {
        node.ensureType(inputType)
        return DataNode.invoke(outputType) {
            ReduceGroupBuilder<T, R>(meta).apply(action).buildGroups(node).forEach { group ->

                //val laminate = Laminate(group.meta, meta)

                val dataMap = group.node.dataSequence().associate { it }

                val groupName: String = group.name

                val groupMeta = group.meta

                val env = ActionEnv(groupName.toName(), groupMeta, meta)

                val res: DynamicData<R> = dataMap.reduce(
                    outputType,
                    meta = groupMeta
                ) { group.result.invoke(env, it) }

                set(env.name, res)
            }

        }
    }
}

operator fun <T> Map<Name, T>.get(name: String) = get(name.toName())
