package hep.dataforge.data

import hep.dataforge.meta.Laminate
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.names.Name
import kotlinx.coroutines.runBlocking


class FragmentEnv<T : Any, R : Any>(val context: Context, val name: String, var meta: MetaBuilder, val log: Chronicle) {
    lateinit var result: suspend (T) -> R

    fun result(f: suspend (T) -> R) {
        result = f;
    }
}


class SplitBuilder<T : Any, R : Any>(val context: Context, val name: String, val meta: Meta) {
    internal val fragments: MutableMap<String, FragmentEnv<T, R>.() -> Unit> = HashMap()

    /**
     * Add new fragment building rule. If the framgent not defined, result won't be available even if it is present in the map
     * @param name the name of a fragment
     * @param rule the rule to transform fragment name and meta using
     */
    fun fragment(name: String, rule: FragmentEnv<T, R>.() -> Unit) {
        fragments[name] = rule
    }
}

class KSplit<T : Any, R : Any>(
    actionName: String,
    inputType: Class<T>,
    outputType: Class<R>,
    private val action: SplitBuilder<T, R>.() -> Unit
) : GenericAction<T, R>(actionName, inputType, outputType) {

    override fun run(context: Context, data: DataNode<out T>, actionMeta: Meta): DataNode<R> {
        if (!this.inputType.isAssignableFrom(data.type)) {
            throw RuntimeException("Type mismatch in action $name. $inputType expected, but ${data.type} received")
        }

        val builder = DataSet.edit(outputType)


        runBlocking {
            data.dataStream(true).forEach {

                val laminate = Laminate(it.meta, actionMeta)

                val split = SplitBuilder<T, R>(context, it.name, it.meta).apply(action)


                val dispatcher = context + getExecutorService(context, laminate).asCoroutineDispatcher()

                // Create a map of results in a single goal
                //val commonGoal = it.goal.pipe(dispatcher) { split.result.invoke(env, it) }

                // apply individual fragment rules to result
                split.fragments.forEach { name, rule ->
                    val env = FragmentEnv<T, R>(
                        context,
                        it.name,
                        laminate.builder,
                        context.history.getChronicle(Name.joinString(it.name, name))
                    )

                    rule.invoke(env)

                    val goal = it.goal.pipe(dispatcher, env.result)

                    val res = NamedData(env.name, outputType, goal, env.meta)
                    builder.add(res)
                }
            }
        }

        return builder.build();
    }
}

inline fun <reified T : Any, reified R : Any> DataNode<T>.pipe(
    context: Context,
    meta: Meta,
    name: String = "pipe",
    noinline action: PipeBuilder<T, R>.() -> Unit
): DataNode<R> {
    return KPipe(name, T::class.java, R::class.java, action).run(context, this, meta);
}