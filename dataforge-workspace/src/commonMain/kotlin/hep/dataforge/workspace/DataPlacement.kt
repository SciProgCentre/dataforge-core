package hep.dataforge.workspace

import hep.dataforge.data.*
import hep.dataforge.meta.*
import hep.dataforge.names.Name
import hep.dataforge.names.plus
import hep.dataforge.names.removeHeadOrNull
import hep.dataforge.names.toName
import kotlinx.coroutines.flow.*
import kotlin.reflect.KClass

public interface DataPlacement: MetaRepr {
    /**
     * Select a placement for a data with given [name] and [meta]. The result is null if data should be ignored.
     */
    public fun place(name: Name, meta: Meta, dataType: KClass<*>): Name?

    public companion object {
        public val ALL: DataPlacement = object : DataPlacement {
            override fun place(name: Name, meta: Meta, dataType: KClass<*>): Name = name

            override fun toMeta(): Meta = Meta{"from" put "*"}
        }

        public fun into(target: Name): DataPlacement = DataPlacementScheme{
            to = target.toString()
        }

        public fun into(target: String): DataPlacement = DataPlacementScheme{
            to = target
        }

    }
}

public fun DataPlacement.place(datum: NamedData<*>): Name? = place(datum.name, datum.meta, datum.type)

private class ArrangedDataSet<T : Any>(
    private val source: DataSet<T>,
    private val placement: DataPlacement,
) : DataSet<T> {
    override val dataType: KClass<out T> get() = source.dataType

    override fun flow(): Flow<NamedData<T>> = source.flow().mapNotNull {
        val newName = placement.place(it) ?: return@mapNotNull null
        it.data.named(newName)
    }

    override suspend fun getData(name: Name): Data<T>? = flow().filter { it.name == name }.firstOrNull()

    override val updates: Flow<Name> = source.updates.flatMapConcat {
        flowChildren(it).mapNotNull(placement::place)
    }
}

public class DataPlacementScheme : Scheme(), DataPlacement {
    /**
     * A source node for the filter
     */
    public var from: String? by string()

    /**
     * A target placement for the filtered node
     */
    public var to: String? by string()

    /**
     * A regular expression pattern for the filter
     */
    public var pattern: String? by string()
//    val prefix by string()
//    val suffix by string()

    override fun place(name: Name, meta: Meta, dataType: KClass<*>): Name? {
        val fromName = from?.toName() ?: Name.EMPTY
        val nameReminder = name.removeHeadOrNull(fromName) ?: return null
        val regex = pattern?.toRegex()
        return if (regex == null || nameReminder.toString().matches(regex)) {
            (to?.toName() ?: Name.EMPTY) + nameReminder
        } else {
            null
        }
    }

    public companion object : SchemeSpec<DataPlacementScheme>(::DataPlacementScheme)
}


/**
 * Apply data node rearrangement
 */
public fun <T : Any> DataSet<T>.rearrange(placement: DataPlacement): DataSet<T> = ArrangedDataSet(this, placement)

///**
// * Mask data using [DataPlacementScheme] specification
// */
//public fun <T : Any> DataSet<T>.rearrange(placement: Meta): DataSet<T> =
//    rearrange(DataPlacementScheme.read(placement))

/**
 * Mask data using [DataPlacementScheme] builder
 */
public fun <T : Any> DataSet<T>.rearrange(placementBuilder: DataPlacementScheme.() -> Unit): DataSet<T> =
    rearrange(DataPlacementScheme(placementBuilder))