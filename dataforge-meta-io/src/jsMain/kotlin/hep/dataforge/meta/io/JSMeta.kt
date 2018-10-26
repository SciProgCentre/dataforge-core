package hep.dataforge.meta.io

import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaItem
import hep.dataforge.meta.Value

/**
 * Represent any js object as meta
 */
class JSMeta(val obj: Any) : Meta {
    override val items: Map<String, MetaItem<out Meta>>
        get() = listKeys(obj).associateWith { convert(js("obj[it]")) }

    private fun listKeys(obj: Any): List<String> = js("Object").keys(obj) as List<String>

    private fun isList(obj: Any): Boolean = js("Array").isArray(obj) as Boolean

    private fun isPrimitive(obj: Any?): Boolean = js("obj !== Object(obj)") as Boolean

    private fun convert(obj: Any?): MetaItem<out Meta> {
        return when (obj) {
            null, isPrimitive(obj), is Number, is String, is Boolean -> MetaItem.ValueItem<JSMeta>(Value.of(obj))
            isList(obj) -> {
                val list = obj as List<*>
                //if first value is primitive, treat as value
                if (isPrimitive(list.first())) {
                    MetaItem.ValueItem<JSMeta>(Value.of(list))
                } else {
                    //else treat as meta list
                    MetaItem.MultiNodeItem(list.map { JSMeta(it!!) })
                }
            }
            else -> MetaItem.SingleNodeItem(JSMeta(obj))
        }
    }
}