package hep.dataforge.meta


//TODO add Meta wrapper for dynamic

/**
 * Represent or copy this [Meta] to dynamic object to be passed to JS libraries
 */
fun Meta.toDynamic(): dynamic {
    //if(this is DynamicMeta) return this.obj

    fun MetaItem<*>.toDynamic(): dynamic = when (this) {
        is MetaItem.ValueItem -> this.value.value.asDynamic()
        is MetaItem.NodeItem -> this.node.toDynamic()
    }

    val res = js("{}")
    this.items.entries.groupBy { it.key.body }.forEach { (key, value) ->
        val list = value.map { it.value }
        res[key] = when (list.size) {
            1 -> list.first().toDynamic()
            else -> list.map { it.toDynamic() }
        }
    }
    return res
}