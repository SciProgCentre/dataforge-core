package space.kscience.dataforge.properties


import space.kscience.dataforge.meta.MutableMeta
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.transformations.MetaConverter
import space.kscience.dataforge.meta.transformations.nullableItemToObject
import space.kscience.dataforge.meta.transformations.nullableObjectToMetaItem
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.Name

@DFExperimental
public class MetaProperty<T : Any>(
    public val meta: MutableMeta,
    public val name: Name,
    public val converter: MetaConverter<T>,
) : Property<T?> {

    override var value: T?
        get() = converter.nullableItemToObject(meta[name])
        set(value) {
            meta[name] = converter.nullableObjectToMetaItem(value)
        }

    override fun onChange(owner: Any?, callback: (T?) -> Unit) {
        meta.onChange(owner) { name, oldItem, newItem ->
            if (name.startsWith(this.name) && oldItem != newItem) callback(converter.nullableItemToObject(newItem))
        }
    }

    override fun removeChangeListener(owner: Any?) {
        meta.removeListener(owner)
    }
}