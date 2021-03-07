package space.kscience.dataforge.actions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.fold
import space.kscience.dataforge.data.*
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.MetaBuilder
import space.kscience.dataforge.misc.DFBuilder
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.misc.DFInternal
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.toName
import kotlin.reflect.KType
import kotlin.reflect.typeOf


public class JoinGroup<T : Any, R : Any>(public var name: String, internal val set: DataSet<T>) {

    public var meta: MetaBuilder = MetaBuilder()

    public lateinit var result: suspend ActionEnv.(Map<Name, T>) -> R

    public fun result(f: suspend ActionEnv.(Map<Name, T>) -> R) {
        this.result = f;
    }

}

@DFBuilder
public class ReduceGroupBuilder<T : Any, R : Any>(
    private val inputType: KType,
    private val scope: CoroutineScope,
    public val actionMeta: Meta,
) {
    private val groupRules: MutableList<suspend (DataSet<T>) -> List<JoinGroup<T, R>>> = ArrayList();

    /**
     * introduce grouping by meta value
     */
    public fun byValue(tag: String, defaultTag: String = "@default", action: JoinGroup<T, R>.() -> Unit) {
        groupRules += { node ->
            GroupRule.byMetaValue(scope, tag, defaultTag).gather(node).map {
                JoinGroup<T, R>(it.key, it.value).apply(action)
            }
        }
    }

    public fun group(
        groupName: String,
        filter: suspend (Name, Data<T>) -> Boolean,
        action: JoinGroup<T, R>.() -> Unit,
    ) {
        groupRules += { source ->
            listOf(
                JoinGroup<T, R>(groupName, source.filter(filter)).apply(action)
            )
        }
    }

    /**
     * Apply transformation to the whole node
     */
    public fun result(resultName: String, f: suspend ActionEnv.(Map<Name, T>) -> R) {
        groupRules += { node ->
            listOf(JoinGroup<T, R>(resultName, node).apply { result(f) })
        }
    }

    internal suspend fun buildGroups(input: DataSet<T>): List<JoinGroup<T, R>> {
        return groupRules.flatMap { it.invoke(input) }
    }

}

@PublishedApi
internal class ReduceAction<T : Any, R : Any>(
    private val inputType: KType,
    outputType: KType,
    private val action: ReduceGroupBuilder<T, R>.() -> Unit,
) : CachingAction<T, R>(outputType) {
    //TODO optimize reduction. Currently the whole action recalculates on push


    override fun CoroutineScope.transform(set: DataSet<T>, meta: Meta, key: Name): Flow<NamedData<R>> = flow {
        ReduceGroupBuilder<T, R>(inputType, this@transform, meta).apply(action).buildGroups(set).forEach { group ->
            val dataFlow: Map<Name, Data<T>> = group.set.flow().fold(HashMap()) { acc, value ->
                acc.apply {
                    acc[value.name] = value.data
                }
            }

            val groupName: String = group.name

            val groupMeta = group.meta

            val env = ActionEnv(groupName.toName(), groupMeta, meta)
            @OptIn(DFInternal::class) val res: Data<R> = dataFlow.reduceToData(
                outputType,
                meta = groupMeta
            ) { group.result.invoke(env, it) }

            emit(res.named(env.name))
        }
    }
}

/**
 * A one-to-one mapping action
 */
@DFExperimental
@Suppress("FunctionName")
public inline fun <reified T : Any, reified R : Any> Action.Companion.reduce(
    noinline builder: ReduceGroupBuilder<T, R>.() -> Unit,
): Action<T, R> = ReduceAction(typeOf<T>(), typeOf<R>(), builder)
