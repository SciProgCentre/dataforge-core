package space.kscience.dataforge.properties

import space.kscience.dataforge.meta.Config
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.set
import space.kscience.dataforge.meta.transformations.MetaConverter
import space.kscience.dataforge.meta.transformations.nullableItemToObject
import space.kscience.dataforge.meta.transformations.nullableObjectToMetaItem
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.Name

@DFExperimental
public class ConfigProperty<T : Any>(
    public val config: Config,
    public val name: Name,
    public val converter: MetaConverter<T>,
) : Property<T?> {

    override var value: T?
        get() = converter.nullableItemToObject(config[name])
        set(value) {
            config[name] = converter.nullableObjectToMetaItem(value)
        }

    override fun onChange(owner: Any?, callback: (T?) -> Unit) {
        config.onChange(owner) { name, oldItem, newItem ->
            if (name == this.name && oldItem != newItem) callback(converter.nullableItemToObject(newItem))
        }
    }

    override fun removeChangeListener(owner: Any?) {
        config.removeListener(owner)
    }
}