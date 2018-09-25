package hep.dataforge.meta.io

import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaItem
import hep.dataforge.meta.ValueType
import hep.dataforge.meta.boolean
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable

/*Universal serialization*/

@Serializable
class MetaProxy(val map: Map<String, @Polymorphic MetaItemProxy>)


@Serializable
sealed class MetaItemProxy {
    @Serializable
    class NumberValueProxy(val number: Number) : MetaItemProxy()

    @Serializable
    class StringValueProxy(val string: String) : MetaItemProxy()

    @Serializable
    class BooleanValueProxy(val boolean: Boolean) : MetaItemProxy()

    @Serializable
    object NullValueProxy : MetaItemProxy()

    @Serializable
    class SingleMetaProxy(val node: MetaProxy) : MetaItemProxy()

    @Serializable
    class MetaListProxy(val list: List<@Polymorphic MetaProxy>) : MetaItemProxy()
}


fun Meta.toMap(): MetaProxy {
    return MetaProxy(this.items.mapValues { (_, value) ->
        when (value) {
            is MetaItem.ValueItem<*> -> when (value.value.type) {
                ValueType.NUMBER -> MetaItemProxy.NumberValueProxy(value.value.number)
                ValueType.STRING -> MetaItemProxy.StringValueProxy(value.value.string)
                ValueType.BOOLEAN -> MetaItemProxy.BooleanValueProxy(value.value.boolean)
                ValueType.NULL -> MetaItemProxy.NullValueProxy
            }
            is MetaItem.SingleNodeItem<*> -> MetaItemProxy.SingleMetaProxy(value.node.toMap())
            is MetaItem.MultiNodeItem<*> -> MetaItemProxy.MetaListProxy(value.nodes.map { it.toMap() })
        }
    })
}
