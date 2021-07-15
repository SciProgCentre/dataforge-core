package space.kscience.dataforge.meta

import space.kscience.dataforge.misc.DFBuilder
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.NameToken
import space.kscience.dataforge.names.asName
import space.kscience.dataforge.values.EnumValue
import space.kscience.dataforge.values.Value
import space.kscience.dataforge.values.asValue
import kotlin.jvm.JvmName

/**
 * DSL builder for meta. Is not intended to store mutable state
 */
@DFBuilder
public class MetaBuilder : AbstractMutableMeta<MetaBuilder>() {
    override val children: MutableMap<NameToken, TypedMetaItem<MetaBuilder>> = LinkedHashMap()

    override fun wrapNode(meta: Meta): MetaBuilder = if (meta is MetaBuilder) meta else meta.toMutableMeta()
    override fun empty(): MetaBuilder = MetaBuilder()

    public infix fun String.put(item: MetaItem?) {
        set(this, item)
    }

    public infix fun String.put(value: Value?) {
        set(this, value)
    }

    public infix fun String.put(string: String?) {
        set(this, string?.asValue())
    }

    public infix fun String.put(number: Number?) {
        set(this, number?.asValue())
    }

    public infix fun String.put(boolean: Boolean?) {
        set(this, boolean?.asValue())
    }

    public infix fun String.put(enum: Enum<*>) {
        set(this, EnumValue(enum))
    }

    @JvmName("putValues")
    public infix fun String.put(iterable: Iterable<Value>) {
        set(this, iterable.asValue())
    }

    @JvmName("putNumbers")
    public infix fun String.put(iterable: Iterable<Number>) {
        set(this, iterable.map { it.asValue() }.asValue())
    }

    @JvmName("putStrings")
    public infix fun String.put(iterable: Iterable<String>) {
        set(this, iterable.map { it.asValue() }.asValue())
    }

    public infix fun String.put(array: DoubleArray) {
        set(this, array.asValue())
    }

    public infix fun String.put(meta: Meta?) {
        this@MetaBuilder[this] = meta
    }

    public infix fun String.put(repr: MetaRepr?) {
        set(this, repr?.toMeta())
    }

    @JvmName("putMetas")
    public infix fun String.put(value: Iterable<Meta>) {
        set(this,value.toList())
    }

    public inline infix fun String.put(metaBuilder: MetaBuilder.() -> Unit) {
        this@MetaBuilder[this] = MetaBuilder().apply(metaBuilder)
    }

    public infix fun Name.put(value: Value?) {
        set(this, value)
    }

    public infix fun Name.put(string: String?) {
        set(this, string?.asValue())
    }

    public infix fun Name.put(number: Number?) {
        set(this, number?.asValue())
    }

    public infix fun Name.put(boolean: Boolean?) {
        set(this, boolean?.asValue())
    }

    public infix fun Name.put(enum: Enum<*>) {
        set(this, EnumValue(enum))
    }

    @JvmName("putValues")
    public infix fun Name.put(iterable: Iterable<Value>) {
        set(this, iterable.asValue())
    }

    public infix fun Name.put(meta: Meta?) {
        this@MetaBuilder[this] = meta
    }

    public infix fun Name.put(repr: MetaRepr?) {
        set(this, repr?.toMeta())
    }

    @JvmName("putMetas")
    public infix fun Name.put(value: Iterable<Meta>) {
        set(this, value.toList())
    }

    public infix fun Name.put(metaBuilder: MetaBuilder.() -> Unit) {
        this@MetaBuilder[this] = MetaBuilder().apply(metaBuilder)
    }
}

/**
 * For safety, builder always copies the initial meta even if it is builder itself
 */
public fun Meta.toMutableMeta(): MetaBuilder {
    return MetaBuilder().also { builder ->
        items.mapValues { entry ->
            val item = entry.value
            builder[entry.key.asName()] = when (item) {
                is MetaItemValue -> item.value
                is MetaItemNode -> MetaItemNode(item.node.toMutableMeta())
            }
        }
    }
}

/**
 * Build a [MetaBuilder] using given transformation
 */
@Suppress("FunctionName")
public inline fun Meta(builder: MetaBuilder.() -> Unit): MetaBuilder = MetaBuilder().apply(builder)