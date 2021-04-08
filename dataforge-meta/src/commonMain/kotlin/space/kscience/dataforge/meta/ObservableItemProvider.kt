package space.kscience.dataforge.meta

import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.toName
import kotlin.reflect.KProperty1


internal data class ItemListener(
    val owner: Any? = null,
    val action: (name: Name, oldItem: MetaItem?, newItem: MetaItem?) -> Unit,
)


public interface ObservableItemProvider : ItemProvider {
    public fun onChange(owner: Any?, action: (name: Name, oldItem: MetaItem?, newItem: MetaItem?) -> Unit)
    public fun removeListener(owner: Any?)
}

public interface ItemPropertyProvider: ObservableItemProvider, MutableItemProvider

/**
 * Use the value of the property in a [callBack].
 * The callback is called once immediately after subscription to pass the initial value.
 *
 * Optional [owner] property is used for
 */
@DFExperimental
public fun <O : ObservableItemProvider, T> O.useProperty(
    property: KProperty1<O, T>,
    owner: Any? = null,
    callBack: O.(T) -> Unit,
) {
    //Pass initial value.
    callBack(property.get(this))
    onChange(owner) { name, oldItem, newItem ->
        if (name == property.name.toName() && oldItem != newItem) {
            callBack(property.get(this))
        }
    }
}