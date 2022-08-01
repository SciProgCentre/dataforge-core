package space.kscience.dataforge.values

import space.kscience.dataforge.meta.set
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.NameToken
import space.kscience.dataforge.names.asName

/**
 * An object that could provide values
 */
public fun interface ValueProvider {
    public fun getValue(name: Name): Value?
}

public fun ValueProvider.getValue(key: String): Value? = getValue(Name.parse(key))

/**
 * An object that could consume values
 */
public interface MutableValueProvider : ValueProvider {
    public fun setValue(name: Name, value: Value?)
}

public fun MutableValueProvider.setValue(key: String, value: Value?) {
    setValue(Name.parse(key), value)
}

//value setters

public operator fun MutableValueProvider.set(name: NameToken, value: Value?): Unit = set(name.asName(), value)
public operator fun MutableValueProvider.set(key: String, value: Value?): Unit = set(Name.parse(key), value)

public operator fun MutableValueProvider.set(name: Name, value: String): Unit = set(name, value.asValue())
public operator fun MutableValueProvider.set(name: NameToken, value: String): Unit = set(name.asName(), value.asValue())
public operator fun MutableValueProvider.set(key: String, value: String): Unit = set(Name.parse(key), value.asValue())

public operator fun MutableValueProvider.set(name: Name, value: Boolean): Unit = set(name, value.asValue())
public operator fun MutableValueProvider.set(name: NameToken, value: Boolean): Unit = set(name.asName(), value.asValue())
public operator fun MutableValueProvider.set(key: String, value: Boolean): Unit = set(Name.parse(key), value.asValue())

public operator fun MutableValueProvider.set(name: Name, value: Number): Unit = set(name, value.asValue())
public operator fun MutableValueProvider.set(name: NameToken, value: Number): Unit = set(name.asName(), value.asValue())
public operator fun MutableValueProvider.set(key: String, value: Number): Unit = set(Name.parse(key), value.asValue())

public operator fun MutableValueProvider.set(name: Name, value: List<Value>): Unit = set(name, value.asValue())
public operator fun MutableValueProvider.set(name: NameToken, value: List<Value>): Unit = set(name.asName(), value.asValue())
public operator fun MutableValueProvider.set(key: String, value: List<Value>): Unit = set(Name.parse(key), value.asValue())
