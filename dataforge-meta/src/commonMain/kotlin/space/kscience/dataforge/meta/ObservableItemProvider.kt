package space.kscience.dataforge.meta

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

public fun <O : ObservableItemProvider, T : Any> O.onChange(
    property: KProperty1<O, T?>,
    owner: Any? = null,
    callBack: O.(T?) -> Unit,
) {
    onChange(null) { name, oldItem, newItem ->
        if (name == property.name.toName() && oldItem != newItem) {
            callBack(property.get(this))
        }
    }
}