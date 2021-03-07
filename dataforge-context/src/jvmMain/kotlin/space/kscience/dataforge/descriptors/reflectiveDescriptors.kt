package space.kscience.dataforge.descriptors


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
