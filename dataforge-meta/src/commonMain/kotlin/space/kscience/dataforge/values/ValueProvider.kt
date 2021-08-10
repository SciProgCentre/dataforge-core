package space.kscience.dataforge.values

import space.kscience.dataforge.names.Name

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