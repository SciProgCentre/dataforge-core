package space.kscience.dataforge.data

import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.MutableMeta


/**
 * Get a metadata node for this set if it is present
 */
public suspend fun DataSet<*>.getMeta(): Meta? = getData(DataSet.META_KEY)?.meta

/**
 * Add meta-data node to a [DataSet]
 */
public suspend fun DataSetBuilder<*>.meta(meta: Meta): Unit = emit(DataSet.META_KEY, Data.empty(meta))

/**
 * Add meta-data node to a [DataSet]
 */
public suspend fun DataSetBuilder<*>.meta(mutableMeta: MutableMeta.() -> Unit): Unit = meta(Meta(mutableMeta))