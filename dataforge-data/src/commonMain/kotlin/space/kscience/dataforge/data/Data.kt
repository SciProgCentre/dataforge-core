package space.kscience.dataforge.data

import kotlinx.coroutines.*
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.MetaRepr
import space.kscience.dataforge.meta.isEmpty
import space.kscience.dataforge.misc.DFInternal
import space.kscience.dataforge.misc.Type
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * A data element characterized by its meta
 */
@Type(Data.TYPE)
public interface Data<out T : Any> : Goal<T>, MetaRepr {
    /**
     * Type marker for the data. The type is known before the calculation takes place so it could be checked.
     */
    public val type: KType

    /**
     * Meta for the data
     */
    public val meta: Meta

    override fun toMeta(): Meta = Meta {
        "type" put (type.toString())
        if (!meta.isEmpty()) {
            "meta" put meta
        }
    }

    public companion object {
        public const val TYPE: String = "data"

        /**
         * The type that can't have any subtypes
         */
        internal val TYPE_OF_NOTHING: KType = typeOf<Unit>()

        public inline fun <reified T : Any> static(
            value: T,
            meta: Meta = Meta.EMPTY,
        ): Data<T> = StaticData(typeOf<T>(), value, meta)

        /**
         * An empty data containing only meta
         */
        @OptIn(DelicateCoroutinesApi::class)
        public fun empty(meta: Meta): Data<Nothing> = object : Data<Nothing> {
            override val type: KType = TYPE_OF_NOTHING
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

/**
 * A lazily computed variant of [Data] based on [LazyGoal]
 * One must ensure that proper [type] is used so this method should not be used
 */
private class LazyData<T : Any>(
    override val type: KType,
    override val meta: Meta = Meta.EMPTY,
    additionalContext: CoroutineContext = EmptyCoroutineContext,
    dependencies: Collection<Data<*>> = emptyList(),
    block: suspend () -> T,
) : Data<T>, LazyGoal<T>(additionalContext, dependencies, block)

public class StaticData<T : Any>(
    override val type: KType,
    value: T,
    override val meta: Meta = Meta.EMPTY,
) : Data<T>, StaticGoal<T>(value)

@Suppress("FunctionName")
@DFInternal
public fun <T : Any> Data(
    type: KType,
    meta: Meta = Meta.EMPTY,
    context: CoroutineContext = EmptyCoroutineContext,
    dependencies: Collection<Data<*>> = emptyList(),
    block: suspend () -> T,
): Data<T> = LazyData(type, meta, context, dependencies, block)

@OptIn(DFInternal::class)
@Suppress("FunctionName")
public inline fun <reified T : Any> Data(
    meta: Meta = Meta.EMPTY,
    context: CoroutineContext = EmptyCoroutineContext,
    dependencies: Collection<Data<*>> = emptyList(),
    noinline block: suspend () -> T,
): Data<T> = Data(typeOf<T>(), meta, context, dependencies, block)
