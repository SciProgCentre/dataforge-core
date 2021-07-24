package space.kscience.dataforge.properties


import space.kscience.dataforge.meta.ObservableMeta
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.toName
import kotlin.reflect.KMutableProperty1

@DFExperimental
public fun <P : ObservableMeta, T : Any> P.property(property: KMutableProperty1<P, T?>): Property<T?> =
    object : Property<T?> {
        override var value: T?
            get() = property.get(this@property)
            set(value) {
                property.set(this@property, value)
            }

        override fun onChange(owner: Any?, callback: (T?) -> Unit) {
            this@property.onChange(this) { name, oldItem, newItem ->
                if (name.startsWith(property.name.toName()) && oldItem != newItem) {
                    callback(property.get(this@property))
                }
            }
        }

        override fun removeChangeListener(owner: Any?) {
            this@property.removeListener(this@property)
        }

    }