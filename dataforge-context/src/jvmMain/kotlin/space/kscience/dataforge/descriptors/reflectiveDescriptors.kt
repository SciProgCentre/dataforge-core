package space.kscience.dataforge.descriptors

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.slf4j.LoggerFactory
import space.kscience.dataforge.meta.Scheme
import space.kscience.dataforge.meta.SchemeSpec
import space.kscience.dataforge.meta.ValueType
import space.kscience.dataforge.meta.descriptors.MetaDescriptor
import space.kscience.dataforge.meta.descriptors.MetaDescriptorBuilder
import space.kscience.dataforge.meta.descriptors.node
import java.net.URL
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.typeOf


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


public fun <T : Any> MetaDescriptor.Companion.forClass(
    kClass: KClass<T>,
    mod: MetaDescriptorBuilder.() -> Unit = {},
): MetaDescriptor = MetaDescriptor {
    when {
        kClass.isSubclassOf(Number::class) -> valueType(ValueType.NUMBER)
        kClass == String::class -> ValueType.STRING
        kClass == Boolean::class -> ValueType.BOOLEAN
        kClass == DoubleArray::class -> ValueType.LIST
    }

    kClass.annotations.forEach {
        when (it) {
            is Description -> description = it.value

            is DescriptorResource -> loadDescriptorFromResource(it)

            is DescriptorUrl -> loadDescriptorFromUrl(URL(it.url))
        }
    }
    kClass.memberProperties.forEach { property ->

        var flag = false

        val descriptor = MetaDescriptor {
            //use base type descriptor as a base
            (property.returnType.classifier as? KClass<*>)?.let {
                from(forClass(it))
            }
            property.annotations.forEach {
                when (it) {
                    is Description -> {
                        description = it.value
                        flag = true
                    }

                    is Multiple -> {
                        multiple = true
                        flag = true
                    }

                    is DescriptorResource -> {
                        loadDescriptorFromResource(it)
                        flag = true
                    }

                    is DescriptorUrl -> {
                        loadDescriptorFromUrl(URL(it.url))
                        flag = true
                    }
                }
            }
        }
        if (flag) {
            node(property.name, descriptor)
        }
    }
    mod()
}

@Suppress("UNCHECKED_CAST")
public inline fun <reified T : Scheme> SchemeSpec<T>.autoDescriptor( noinline mod: MetaDescriptorBuilder.() -> Unit = {}): MetaDescriptor =
    MetaDescriptor.forClass(typeOf<T>().classifier as KClass<T>, mod)