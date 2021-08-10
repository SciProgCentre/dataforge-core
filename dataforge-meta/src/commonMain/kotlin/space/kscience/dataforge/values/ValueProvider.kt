package space.kscience.dataforge.values

import space.kscience.dataforge.names.Name

/**
 * An object that could provide values
 */
public fun interface ValueProvider {
    public fun getValue(name: Name): Value?
}

/**
 * An object that could consume values
 */
public interface MutableValueProvider: ValueProvider{
    public fun setValue(name: Name, value: Value?)
}