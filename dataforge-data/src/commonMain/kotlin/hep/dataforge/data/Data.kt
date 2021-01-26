package hep.dataforge.data

import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaRepr
import hep.dataforge.meta.isEmpty
import hep.dataforge.misc.Type
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.reflect.KClass

/**
 * A data element characterized by its meta
 */
@Type(Data.TYPE)
public interface Data<out T : Any> : Goal<T>, MetaRepr {
    /**
     * Type marker for the data. The type is known before the calculation takes place so it could be checked.
     */
    public val type: KClass<out T>

    /**
     * Meta for the data
     */
    public val meta: Meta

    override fun toMeta(): Meta = Meta {
        "type" put (type.simpleName ?: "undefined")
        if (!meta.isEmpty()) {
            "meta" put meta
        }
    }

    public companion object {
        public const val TYPE: String = "data"

        public fun <T : Any> static(
            value: T,
            meta: Meta = Meta.EMPTY,
        ): Data<T> = StaticData(value, meta)

        /**
         * An empty data containing only meta
         */
        public fun empty(meta: Meta): Data<Nothing> = object : Data<Nothing> {
            override val type: KClass<out Nothing> = Nothing::class
            override val meta: Meta = meta
            override val dependencies: Collection<Goal<*>> = emptyList()
            override val deferred: Deferred<Nothing>
                get() = GlobalScope.async(start = CoroutineStart.LAZY) {
                    error("The Data is empty and could not be computed")
                }

            override fun async(coroutineScope: CoroutineScope): Deferred<Nothing> = deferred
            override fun reset() {}
        }
    }
}

public class LazyData<T : Any>(
    override val type: KClass<out T>,
    override val meta: Meta = Meta.EMPTY,
    context: CoroutineContext = EmptyCoroutineContext,
    dependencies: Collection<Data<*>> = emptyList(),
    block: suspend () -> T,
) : Data<T>, LazyGoal<T>(context, dependencies, block)

public class StaticData<T : Any>(
    value: T,
    override val meta: Meta = Meta.EMPTY,
) : Data<T>, StaticGoal<T>(value) {
    override val type: KClass<out T> get() = value::class
}

@Suppress("FunctionName")
public fun <T : Any> Data(
    type: KClass<out T>,
    meta: Meta = Meta.EMPTY,
    context: CoroutineContext = EmptyCoroutineContext,
    dependencies: Collection<Data<*>> = emptyList(),
    block: suspend () -> T,
): Data<T> = LazyData(type, meta, context, dependencies, block)

@Suppress("FunctionName")
public inline fun <reified T : Any> Data(
    meta: Meta = Meta.EMPTY,
    context: CoroutineContext = EmptyCoroutineContext,
    dependencies: Collection<Data<*>> = emptyList(),
    noinline block: suspend () -> T,
): Data<T> = Data(T::class, meta, context, dependencies, block)
