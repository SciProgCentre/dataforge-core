package space.kscience.dataforge.properties


import space.kscience.dataforge.meta.Scheme
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.startsWith
import kotlin.reflect.KMutableProperty1

@DFExperimental
public fun <S : Scheme, T : Any> S.property(property: KMutableProperty1<S, T?>): Property<T?> =
    object : Property<T?> {
        override var value: T?
            get() = property.get(this@property)
            set(value) {
                property.set(this@property, value)
            }

        override fun onChange(owner: Any?, callback: (T?) -> Unit) {
            this@property.meta.onChange(this) { name ->
                if (name.startsWith(Name.parse(property.name))) {
                    callback(property.get(this@property))
                }
            }
        }

        override fun removeChangeListener(owner: Any?) {
            this@property.meta.removeListener(this@property)
        }

    }