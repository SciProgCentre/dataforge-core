package hep.dataforge.meta

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/*
 * Extra delegates for special cases
 */

/**
 * A delegate for a string list
 */
class StringListConfigDelegate(
    val config: Config,
    private val key: String? = null,
    private val default: List<String> = emptyList()
) :
    ReadWriteProperty<Any?, List<String>> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): List<String> {
        return config[key ?: property.name]?.value?.list?.map { it.string } ?: default
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: List<String>) {
        val name = key ?: property.name
        config[name] = value
    }
}

fun Configurable.stringList(vararg default: String = emptyArray(), key: String? = null) =
    StringListConfigDelegate(config, key, default.toList())


fun <T : Metoid> Metoid.child(key: String? = null, converter: (Meta) -> T) = ChildDelegate(meta, key, converter)

fun <T : Configurable> Configurable.child(key: String? = null, converter: (Meta) -> T) =
    MutableMorphDelegate(config, key, converter)
