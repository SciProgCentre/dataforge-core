package hep.dataforge.meta

import hep.dataforge.names.Name
import hep.dataforge.names.asName
import hep.dataforge.values.EnumValue
import hep.dataforge.values.Value
import hep.dataforge.values.asValue
import kotlin.jvm.JvmName

/**
 * DSL builder for meta. Is not intended to store mutable state
 */
@DFBuilder
public class MetaBuilder : AbstractMutableMeta<MetaBuilder>() {
    override fun wrapNode(meta: Meta): MetaBuilder = if (meta is MetaBuilder) meta else meta.builder()
    override fun empty(): MetaBuilder = MetaBuilder()

    public infix fun String.put(item: MetaItem<*>?) {
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

    public infix fun String.put(metaBuilder: MetaBuilder.() -> Unit) {
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
public fun Meta.builder(): MetaBuilder {
    return MetaBuilder().also { builder ->
        items.mapValues { entry ->
            val item = entry.value
            builder[entry.key.asName()] = when (item) {
                is MetaItem.ValueItem -> item.value
                is MetaItem.NodeItem -> MetaItem.NodeItem(item.node.builder())
            }
        }
    }
}

/**
 * Create a deep copy of this meta and apply builder to it
 */
public inline fun Meta.edit(builder: MetaBuilder.() -> Unit): MetaBuilder = builder().apply(builder)

/**
 * Build a [MetaBuilder] using given transformation
 */
@Suppress("FunctionName")
public inline fun Meta(builder: MetaBuilder.() -> Unit): MetaBuilder = MetaBuilder().apply(builder)