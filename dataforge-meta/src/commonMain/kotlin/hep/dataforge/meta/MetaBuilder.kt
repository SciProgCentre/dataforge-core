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
class MetaBuilder : AbstractMutableMeta<MetaBuilder>() {
    override fun wrapNode(meta: Meta): MetaBuilder = if (meta is MetaBuilder) meta else meta.builder()
    override fun empty(): MetaBuilder = MetaBuilder()

    infix fun String.put(value: Value){
        set(this,value)
    }

    infix fun String.put(string: String){
        set(this,string.asValue())
    }

    infix fun String.put(number: Number){
        set(this,number.asValue())
    }

    infix fun String.put(boolean: Boolean){
        set(this, boolean.asValue())
    }

    infix fun String.put(enum: Enum<*>){
        set(this, EnumValue(enum))
    }

    @JvmName("putValues")
    infix fun String.put(iterable: Iterable<Value>){
        set(this, iterable.asValue())
    }

    @JvmName("putNumbers")
    infix fun String.put(iterable: Iterable<Number>){
        set(this, iterable.map { it.asValue() }.asValue())
    }

    @JvmName("putStrings")
    infix fun String.put(iterable: Iterable<String>){
        set(this, iterable.map { it.asValue() }.asValue())
    }

    infix fun String.put(array: DoubleArray){
        set(this, array.asValue())
    }

    infix fun String.putValue(any: Any?){
        set(this, Value.of(any))
    }

    infix fun String.put(meta: Meta) {
        this@MetaBuilder[this] = meta
    }

    infix fun String.put(repr: MetaRepr){
        set(this,repr.toMeta())
    }

    @JvmName("putMetas")
    infix fun String.put(value: Iterable<Meta>) {
        this@MetaBuilder[this] = value.toList()
    }

    infix fun String.put(metaBuilder: MetaBuilder.() -> Unit) {
        this@MetaBuilder[this] = MetaBuilder().apply(metaBuilder)
    }

    infix fun Name.put(value: Value){
        set(this,value)
    }

    infix fun Name.put(string: String){
        set(this,string.asValue())
    }

    infix fun Name.put(number: Number){
        set(this,number.asValue())
    }

    infix fun Name.put(boolean: Boolean){
        set(this, boolean.asValue())
    }

    infix fun Name.put(enum: Enum<*>){
        set(this, EnumValue(enum))
    }

    @JvmName("putValues")
    infix fun Name.put(iterable: Iterable<Value>){
        set(this, iterable.asValue())
    }

    infix fun Name.put(meta: Meta) {
        this@MetaBuilder[this] = meta
    }

    infix fun Name.put(repr: MetaRepr){
        set(this,repr.toMeta())
    }

    @JvmName("putMetas")
    infix fun Name.put(value: Iterable<Meta>) {
        this@MetaBuilder[this] = value.toList()
    }

    infix fun Name.put(metaBuilder: MetaBuilder.() -> Unit) {
        this@MetaBuilder[this] = MetaBuilder().apply(metaBuilder)
    }
}

/**
 * For safety, builder always copies the initial meta even if it is builder itself
 */
fun Meta.builder(): MetaBuilder {
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
fun Meta.edit(builder: MetaBuilder.() -> Unit): MetaBuilder = builder().apply(builder)

/**
 * Build a [MetaBuilder] using given transformation
 */
fun buildMeta(builder: MetaBuilder.() -> Unit): MetaBuilder = MetaBuilder().apply(builder)

/**
 * Build meta using given source meta as a base
 */
fun buildMeta(source: Meta, builder: MetaBuilder.() -> Unit): MetaBuilder = source.builder().apply(builder)