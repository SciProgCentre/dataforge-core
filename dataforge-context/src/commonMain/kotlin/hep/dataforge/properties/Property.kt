package hep.dataforge.properties

import hep.dataforge.meta.DFExperimental
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@DFExperimental
public interface Property<T> {
    public var value: T

    public fun onChange(owner: Any? = null, callback: (T) -> Unit)
    public fun removeChangeListener(owner: Any? = null)
}

@DFExperimental
@OptIn(ExperimentalCoroutinesApi::class)
public fun <T> Property<T>.toFlow(): StateFlow<T> = MutableStateFlow(value).also { stateFlow ->
    onChange {
        stateFlow.value = it
    }
}

/**
 * Reflect all changes in the [source] property onto this property
 *
 * @return a mirroring job
 */
@DFExperimental
public fun <T> Property<T>.mirror(source: Property<T>, scope: CoroutineScope) {
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