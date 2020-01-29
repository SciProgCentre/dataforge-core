package hep.dataforge.descriptors

import hep.dataforge.meta.*
import hep.dataforge.meta.descriptors.ItemDescriptor
import hep.dataforge.meta.descriptors.NodeDescriptor
import hep.dataforge.meta.descriptors.attributes
import hep.dataforge.meta.scheme.ConfigurableDelegate
import hep.dataforge.meta.scheme.Scheme
import hep.dataforge.values.parseValue
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties


//inline fun <reified T : Scheme> T.buildDescriptor(): NodeDescriptor = NodeDescriptor {
//    T::class.apply {
//        findAnnotation<ItemDef>()?.let { def ->
//            info = def.info
//            required = def.required
//            multiple = def.multiple
//        }
//        findAnnotation<Attribute>()?.let { attr ->
//            attributes {
//                this[attr.key] = attr.value.parseValue()
//            }
//        }
//        findAnnotation<Attributes>()?.attrs?.forEach { attr ->
//            attributes {
//                this[attr.key] = attr.value.parseValue()
//            }
//        }
//    }
//    T::class.memberProperties.forEach { property ->
//        val delegate = property.getDelegate(this@buildDescriptor)
//
//        val descriptor: ItemDescriptor = when (delegate) {
//            is ConfigurableDelegate -> buildPropertyDescriptor(property, delegate)
//            is ReadWriteDelegateWrapper<*, *> -> {
//                if (delegate.delegate is ConfigurableDelegate) {
//                    buildPropertyDescriptor(property, delegate.delegate as ConfigurableDelegate)
//                } else {
//                    return@forEach
//                }
//            }
//            else -> return@forEach
//        }
//        defineItem(property.name, descriptor)
//    }
//}

//inline fun <T : Scheme, reified V : Any?> buildPropertyDescriptor(
//    property: KProperty1<T, V>,
//    delegate: ConfigurableDelegate
//): ItemDescriptor {
//    when {
//        V::class.isSubclassOf(Scheme::class) -> NodeDescriptor {
//            default = delegate.default.node
//        }
//        V::class.isSubclassOf(Meta::class) -> NodeDescriptor {
//            default = delegate.default.node
//        }
//
//    }
//}
