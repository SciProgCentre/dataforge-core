package space.kscience.dataforge.properties


import space.kscience.dataforge.meta.*
import space.kscience.dataforge.meta.transformations.MetaConverter
import space.kscience.dataforge.meta.transformations.nullableMetaToObject
import space.kscience.dataforge.meta.transformations.nullableObjectToMeta
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.startsWith

@DFExperimental
public class MetaProperty<T : Any>(
    public val meta: ObservableMutableMeta,
    public val name: Name,
    public val converter: MetaConverter<T>,
) : Property<T?> {

    override var value: T?
        get() = converter.nullableMetaToObject(meta[name])
        set(value) {
            meta[name] = converter.nullableObjectToMeta(value) ?: Meta.EMPTY
        }

    override fun onChange(owner: Any?, callback: (T?) -> Unit) {
        meta.onChange(owner) { name ->
            if (name.startsWith(this@MetaProperty.name)) callback(converter.nullableMetaToObject(get(name)))
        }
    }

    override fun removeChangeListener(owner: Any?) {
        meta.removeListener(owner)
    }
}