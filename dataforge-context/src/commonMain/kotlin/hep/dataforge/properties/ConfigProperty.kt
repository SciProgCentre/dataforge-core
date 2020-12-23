package hep.dataforge.properties

import hep.dataforge.meta.Config
import hep.dataforge.meta.DFExperimental
import hep.dataforge.meta.get
import hep.dataforge.meta.set
import hep.dataforge.meta.transformations.MetaConverter
import hep.dataforge.meta.transformations.nullableItemToObject
import hep.dataforge.meta.transformations.nullableObjectToMetaItem
import hep.dataforge.names.Name

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