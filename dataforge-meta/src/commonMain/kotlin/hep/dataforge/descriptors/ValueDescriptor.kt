/*
 * Copyright  2018 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.description

import hep.dataforge.Named
import hep.dataforge.meta.*
import hep.dataforge.names.AnonymousNotAlowed
import hep.dataforge.values.BooleanValue
import hep.dataforge.values.Value
import hep.dataforge.values.ValueType

/**
 * A descriptor for meta value
 *
 * Descriptor can have non-atomic path. It is resolved when descriptor is added to the node
 *
 * @author Alexander Nozik
 */
class ValueDescriptor(val meta: Meta) : MetaRepr {

    /**
     * The default for this value. Null if there is no default.
     *
     * @return
     */
    val default by meta.value()

    /**
     * True if multiple values with this name are allowed.
     *
     * @return
     */
    val multiple: Boolean by meta.boolean(false)

    /**
     * True if the value is required
     *
     * @return
     */
    val required: Boolean by meta.boolean(default == null)

    /**
     * Value name
     *
     * @return
     */
    val name: String by meta.string{ error("Anonimous descriptors are not allowed")}

    /**
     * The value info
     *
     * @return
     */
    val info: String by stringValue(def = "")

    /**
     * A list of allowed ValueTypes. Empty if any value type allowed
     *
     * @return
     */
    val type: List<ValueType> by customValue(def = emptyList()) {
        it.list.map { v -> ValueType.valueOf(v.string) }
    }

    val tags: List<String> by customValue(def = emptyList()) {
        meta.getStringArray("tags").toList()
    }

    /**
     * Check if given value is allowed for here. The type should be allowed and
     * if it is value should be within allowed values
     *
     * @param value
     * @return
     */
    fun isValueAllowed(value: Value): Boolean {
        return (type.isEmpty() || type.contains(ValueType.STRING) || type.contains(value.type)) && (allowedValues.isEmpty() || allowedValues.contains(
            value
        ))
    }

    /**
     * A list of allowed values with descriptions. If empty than any value is
     * allowed.
     *
     * @return
     */
    val allowedValues: List<Value> by customValue(
        def = if (type.size == 1 && type[0] === ValueType.BOOLEAN) {
            listOf(BooleanValue.TRUE, BooleanValue.FALSE)
        } else {
            emptyList()
        }
    ) { it.list }

    companion object {

        /**
         * Build a value descriptor from annotation
         */
        fun build(def: ValueDef): ValueDescriptor {
            val builder = MetaBuilder("value")
                .setValue("name", def.key)

            if (def.type.isNotEmpty()) {
                builder.setValue("type", def.type)
            }

            if (def.multiple) {
                builder.setValue("multiple", def.multiple)
            }

            if (!def.info.isEmpty()) {
                builder.setValue("info", def.info)
            }

            if (def.allowed.isNotEmpty()) {
                builder.setValue("allowedValues", def.allowed)
            } else if (def.enumeration != Any::class) {
                if (def.enumeration.java.isEnum) {
                    val values = def.enumeration.java.enumConstants
                    builder.setValue("allowedValues", values.map { it.toString() })
                } else {
                    throw RuntimeException("Only enumeration classes are allowed in 'enumeration' annotation property")
                }
            }

            if (def.def.isNotEmpty()) {
                builder.setValue("default", def.def)
            } else if (!def.required) {
                builder.setValue("required", def.required)
            }

            if (def.tags.isNotEmpty()) {
                builder.setValue("tags", def.tags)
            }
            return ValueDescriptor(builder)
        }

        /**
         * Build a value descriptor from its fields
         */
        fun build(
            name: String,
            info: String = "",
            defaultValue: Any? = null,
            required: Boolean = false,
            multiple: Boolean = false,
            types: List<ValueType> = emptyList(),
            allowedValues: List<Any> = emptyList()
        ): ValueDescriptor {
            val valueBuilder = buildMeta("value") {
                "name" to name
                if (!types.isEmpty()) "type" to types
                if (required) "required" to required
                if (multiple) "multiple" to multiple
                if (!info.isEmpty()) "info" to info
                if (defaultValue != null) "default" to defaultValue
                if (!allowedValues.isEmpty()) "allowedValues" to allowedValues
            }.build()
            return ValueDescriptor(valueBuilder)
        }

        /**
         * Build empty value descriptor
         */
        fun empty(valueName: String): ValueDescriptor {
            val builder = MetaBuilder("value")
                .setValue("name", valueName)
            return ValueDescriptor(builder)
        }

        /**
         * Merge two separate value descriptors
         */
        fun merge(primary: ValueDescriptor, secondary: ValueDescriptor): ValueDescriptor {
            return ValueDescriptor(Laminate(primary.meta, secondary.meta))
        }
    }
}
