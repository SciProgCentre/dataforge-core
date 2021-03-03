package space.kscience.dataforge.properties

import org.w3c.dom.HTMLInputElement
import space.kscience.dataforge.misc.DFExperimental

@DFExperimental
public fun HTMLInputElement.bindValue(property: Property<String>) {
    if (this.onchange != null) error("Input element already bound")
    this.onchange = {
        property.value = this.value
        Unit
    }
    property.onChange(this) {
        if (value != it) {
            value = it
        }
    }
}

@DFExperimental
public fun HTMLInputElement.bindChecked(property: Property<Boolean>) {
    if (this.onchange != null) error("Input element already bound")
    this.onchange = {
        property.value = this.checked
        Unit
    }
    property.onChange(this) {
        if (checked != it) {
            checked = it
        }
    }
}