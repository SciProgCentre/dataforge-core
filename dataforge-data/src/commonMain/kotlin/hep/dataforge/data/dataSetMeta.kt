package hep.dataforge.data

import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder


/**
 * Get a metadata node for this set if it is present
 */
public suspend fun DataSet<*>.getMeta(): Meta? = getData(DataSet.META_KEY)?.meta

/**
 * Add meta-data node to a [DataSet]
 */
public fun DataSetBuilder<*>.meta(meta: Meta): Unit = set(DataSet.META_KEY, Data.empty(meta))

/**
 * Add meta-data node to a [DataSet]
 */
public fun DataSetBuilder<*>.meta(metaBuilder: MetaBuilder.() -> Unit): Unit = meta(Meta(metaBuilder))