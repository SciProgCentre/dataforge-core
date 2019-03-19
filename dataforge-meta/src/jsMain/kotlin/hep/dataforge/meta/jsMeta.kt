package hep.dataforge.meta




fun Meta.toDynamic(): dynamic {
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