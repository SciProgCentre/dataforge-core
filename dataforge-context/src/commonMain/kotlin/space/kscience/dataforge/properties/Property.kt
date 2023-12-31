package space.kscience.dataforge.properties

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import space.kscience.dataforge.misc.DFExperimental

@DFExperimental
public interface Property<T> {
    public var value: T

    public fun onChange(owner: Any? = null, callback: (T) -> Unit)
    public fun removeChangeListener(owner: Any? = null)
}

@DFExperimental
public fun <T> Property<T>.toFlow(): StateFlow<T> = MutableStateFlow(value).also { stateFlow ->
    onChange {
        stateFlow.value = it
    }
}

/**
 * Reflect all changes in the [source] property onto this property. Does not reflect changes back.
 *
 * @return a mirroring job
 */
@DFExperimental
public fun <T> Property<T>.mirror(source: Property<T>) {
    source.onChange(this) {
        this.value = it
    }
}

/**
 * Bi-directional connection between properties
 */
@DFExperimental
public fun <T> Property<T>.bind(other: Property<T>) {
    onChange(other) {
        other.value = it
    }
    other.onChange {
        this.value = it
    }
}