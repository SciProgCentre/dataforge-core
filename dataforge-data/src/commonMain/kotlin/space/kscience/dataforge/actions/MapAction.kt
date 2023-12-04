package space.kscience.dataforge.actions

import space.kscience.dataforge.data.*
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.MutableMeta
import space.kscience.dataforge.meta.seal
import space.kscience.dataforge.meta.toMutableMeta
import space.kscience.dataforge.misc.DFBuilder
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.misc.DFInternal
import space.kscience.dataforge.names.Name
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Action environment includes data name, data meta and action configuration meta
 */
public data class ActionEnv(
    val name: Name,
    val meta: Meta,
    val actionMeta: Meta,
)

/**
 * Action environment
 */
@DFBuilder
public class MapActionBuilder<T, R>(
    public var name: Name,
    public var meta: MutableMeta,
    public val actionMeta: Meta,
    @PublishedApi internal var outputType: KType,
) {

    public lateinit var result: suspend ActionEnv.(T) -> R

    /**
     * Set unsafe [outputType] for the resulting data. Be sure that it is correct.
     */
    public fun <R1 : R> result(outputType: KType, f: suspend ActionEnv.(T) -> R1) {
        this.outputType = outputType
        result = f;
    }

    /**
     * Calculate the result of goal
     */
    public inline fun <reified R1 : R> result(noinline f: suspend ActionEnv.(T) -> R1) {
        outputType = typeOf<R1>()
        result = f;
    }
}

@PublishedApi
internal class MapAction<in T : Any, R : Any>(
    outputType: KType,
    private val block: MapActionBuilder<T, R>.() -> Unit,
) : AbstractAction<T, R>(outputType) {

    private fun DataSetBuilder<R>.mapOne(name: Name, data: Data<T>, meta: Meta) {
        // Creating a new environment for action using **old** name, old meta and task meta
        val env = ActionEnv(name, data.meta, meta)

        //applying transformation from builder
        val builder = MapActionBuilder<T, R>(
            name,
            data.meta.toMutableMeta(), // using data meta
            meta,
            outputType
        ).apply(block)

        //getting new name
        val newName = builder.name

        //getting new meta
        val newMeta = builder.meta.seal()

        @OptIn(DFInternal::class)
        val newData = Data(builder.outputType, newMeta, dependencies = listOf(data)) {
            builder.result(env, data.await())
        }
        //setting the data node
        data(newName, newData)
    }

    override fun DataSetBuilder<R>.generate(data: DataSet<T>, meta: Meta) {
        data.forEach { mapOne(it.name, it.data, meta) }
    }

    override fun DataSourceBuilder<R>.update(dataSet: DataSet<T>, meta: Meta, updateKey: Name) {
        remove(updateKey)
        dataSet[updateKey]?.let { mapOne(updateKey, it, meta) }
    }
}


/**
 * A one-to-one mapping action
 */
@DFExperimental
public inline fun <T : Any, reified R : Any> Action.Companion.mapping(
    noinline builder: MapActionBuilder<T, R>.() -> Unit,
): Action<T, R> = MapAction(typeOf<R>(), builder)


