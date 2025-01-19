package space.kscience.dataforge.meta.descriptors

import space.kscience.dataforge.meta.Scheme
import space.kscience.dataforge.meta.SchemeSpec
import space.kscience.dataforge.meta.ValueType
import kotlin.reflect.KProperty1
import kotlin.reflect.typeOf

/**
 * Add a value item to a [MetaDescriptor] inferring some of its properties from the type
 */
public inline fun <S : Scheme, reified T> MetaDescriptorBuilder.value(
    property: KProperty1<S, T>,
    noinline block: MetaDescriptorBuilder.() -> Unit = {},
): Unit = when (typeOf<T>()) {
    typeOf<Number>(), typeOf<Int>(), typeOf<Double>(), typeOf<Short>(), typeOf<Long>(), typeOf<Float>() ->
        value(property.name, ValueType.NUMBER) {
            block()
        }
    typeOf<Number?>(), typeOf<Int?>(), typeOf<Double?>(), typeOf<Short?>(), typeOf<Long?>(), typeOf<Float?>() ->
        value(property.name, ValueType.NUMBER) {
            block()
        }
    typeOf<Boolean>() -> value(property.name, ValueType.BOOLEAN) {
        block()
    }
    typeOf<List<Number>>(), typeOf<List<Int>>(), typeOf<List<Double>>(), typeOf<List<Short>>(), typeOf<List<Long>>(), typeOf<List<Float>>(),
    typeOf<IntArray>(), typeOf<DoubleArray>(), typeOf<ShortArray>(), typeOf<LongArray>(), typeOf<FloatArray>(),
    -> value(property.name, ValueType.NUMBER) {
        multiple = true
        block()
    }
    typeOf<String>() -> value(property.name, ValueType.STRING) {
        block()
    }
    typeOf<List<String>>(), typeOf<Array<String>>() -> value(property.name, ValueType.STRING) {
        multiple = true
        block()
    }
    else -> node(property.name, block)
}

/**
 * Add a schem-based branch to a [MetaDescriptor]
 */
public inline fun <S : Scheme, reified T : Scheme> MetaDescriptorBuilder.scheme(
    property: KProperty1<S, T>,
    spec: SchemeSpec<T>,
    noinline block: MetaDescriptorBuilder.() -> Unit = {},
) {
    node(property.name, spec, block)
}