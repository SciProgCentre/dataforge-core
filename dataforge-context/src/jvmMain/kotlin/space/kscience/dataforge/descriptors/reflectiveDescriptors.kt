package space.kscience.dataforge.descriptors

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.slf4j.LoggerFactory
import space.kscience.dataforge.meta.descriptors.MetaDescriptor
import space.kscience.dataforge.meta.descriptors.MetaDescriptorBuilder
import space.kscience.dataforge.misc.DFExperimental
import java.net.URL
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KProperty

/**
 * Description text for meta property, node or whole object
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
public annotation class Description(val value: String)

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
public annotation class Multiple()

@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
public annotation class DescriptorResource(val resourceName: String)

@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
public annotation class DescriptorUrl(val url: String)


@OptIn(ExperimentalSerializationApi::class)
private fun MetaDescriptorBuilder.loadDescriptorFromUrl(url: URL) {
    url.openStream().use {
        from(Json.decodeFromStream(MetaDescriptor.serializer(), it))
    }
}

private fun MetaDescriptorBuilder.loadDescriptorFromResource(resource: DescriptorResource) {
    val url = {}.javaClass.getResource(resource.resourceName)
    if (url != null) {
        loadDescriptorFromUrl(url)
    } else {
        LoggerFactory.getLogger("System")
            .error("Can't find descriptor resource with name ${resource.resourceName}")
    }
}

@DFExperimental
public fun MetaDescriptorBuilder.forAnnotatedElement(element: KAnnotatedElement) {
    element.annotations.forEach {
        when (it) {
            is Description -> description = it.value

            is DescriptorResource -> loadDescriptorFromResource(it)

            is DescriptorUrl -> loadDescriptorFromUrl(URL(it.url))
        }
    }
}

@DFExperimental
public fun MetaDescriptorBuilder.forProperty(property: KProperty<*>) {
    property.annotations.forEach {
        when (it) {
            is Description -> description = it.value

            is DescriptorResource -> loadDescriptorFromResource(it)

            is DescriptorUrl -> loadDescriptorFromUrl(URL(it.url))
        }
    }
}
//
//@DFExperimental
//public fun <T : Scheme> MetaDescriptor.Companion.forScheme(
//    spec: SchemeSpec<T>,
//    mod: MetaDescriptorBuilder.() -> Unit = {},
//): MetaDescriptor = MetaDescriptor {
//    val scheme = spec.empty()
//    val kClass: KClass<T> = scheme::class as KClass<T>
//    when {
//        kClass.isSubclassOf(Number::class) -> valueType(ValueType.NUMBER)
//        kClass == String::class -> ValueType.STRING
//        kClass == Boolean::class -> ValueType.BOOLEAN
//        kClass == DoubleArray::class -> ValueType.LIST
//        kClass == ByteArray::class -> ValueType.LIST
//    }
//
//    forAnnotatedElement(kClass)
//    kClass.memberProperties.forEach { property ->
//        node(property.name) {
//
//            (property.getDelegate(scheme) as? MetaDelegate<*>)?.descriptor?.let {
//                from(it)
//            }
//
//            property.annotations.forEach {
//                when (it) {
//                    is Description -> {
//                        description = it.value
//                    }
//
//                    is Multiple -> {
//                        multiple = true
//                    }
//
//                    is DescriptorResource -> {
//                        loadDescriptorFromResource(it)
//                    }
//
//                    is DescriptorUrl -> {
//                        loadDescriptorFromUrl(URL(it.url))
//                    }
//                }
//            }
//        }
//
//    }
//    mod()
//}
//
//@DFExperimental
//public inline fun <reified T : Scheme> SchemeSpec<T>.autoDescriptor(
//    noinline mod: MetaDescriptorBuilder.() -> Unit = {},
//): MetaDescriptor = MetaDescriptor.forScheme(this, mod)