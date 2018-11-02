package hep.dataforge.meta.io

import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaItem
import hep.dataforge.meta.Value
import hep.dataforge.names.NameToken

/**
 * Represent any js object as meta
 */
class JSMeta(val obj: Any) : Meta {
    override val items: Map<NameToken, MetaItem<out Meta>>
        get() = listKeys(obj).map { NameToken(it) }.associateWith { convert(js("obj[it]")) }

    private fun listKeys(obj: Any): List<String> = js("Object").keys(obj) as List<String>

    private fun isList(obj: Any): Boolean = js("Array").isArray(obj) as Boolean

    private fun isPrimitive(obj: Any?): Boolean = js("obj !== Object(obj)") as Boolean

    private fun convert(obj: Any?): MetaItem<out Meta> {
        return when (obj) {
            null, isPrimitive(obj), is Number, is String, is Boolean -> MetaItem.ValueItem<JSMeta>(Value.of(obj))
            isList(obj) -> {
                val list = obj as List<*>
                MetaItem.ValueItem<JSMeta>(Value.of(list))
            }
            else -> MetaItem.NodeItem(JSMeta(obj))
        }
    }
}