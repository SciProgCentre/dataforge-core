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

package hep.dataforge.descriptors

import hep.dataforge.meta.*
import hep.dataforge.values.*

/**
 * A descriptor for meta value
 *
 * Descriptor can have non-atomic path. It is resolved when descriptor is added to the node
 *
 * @author Alexander Nozik
 */
class ValueDescriptor(override val config: Config) : Specification {

    /**
     * The default for this value. Null if there is no default.
     *
     * @return
     */
    var default: Value? by value()

    /**
     * True if multiple values with this name are allowed.
     *
     * @return
     */
    var multiple: Boolean by boolean(false)

    /**
     * True if the value is required
     *
     * @return
     */
    var required: Boolean by boolean { default == null }

    /**
     * Value name
     *
     * @return
     */
    var name: String by string { error("Anonymous descriptors are not allowed") }

    /**
     * The value info
     *
     * @return
     */
    var info: String? by string()

    /**
     * A list of allowed ValueTypes. Empty if any value type allowed
     *
     * @return
     */
    var type: List<ValueType> by value().map {
        it?.list?.map { v -> ValueType.valueOf(v.string) } ?: emptyList()
    }

    var tags: List<String> by value().map { value ->
        value?.list?.map { it.string } ?: emptyList()
    }

    /**
     * Check if given value is allowed for here. The type should be allowed and
     * if it is value should be within allowed values
     *
     * @param value
     * @return
     */
    fun isAllowedValue(value: Value): Boolean {
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
    var allowedValues: List<Value> by value().map {
        it?.list ?: if (type.size == 1 && type[0] === ValueType.BOOLEAN) {
            listOf(True, False)
        } else {
            emptyList()
        }
    }

    companion object : SpecificationCompanion<ValueDescriptor> {

        override fun wrap(config: Config): ValueDescriptor = ValueDescriptor(config)

//        /**
//         * Build a value descriptor from annotation
//         */
//        fun build(def: ValueDef): ValueDescriptor {
//            val builder = MetaBuilder("value")
//                .setValue("name", def.key)
//
//            if (def.type.isNotEmpty()) {
//                builder.setValue("type", def.type)
//            }
//
//            if (def.multiple) {
//                builder.setValue("multiple", def.multiple)
//            }
//
//            if (!def.info.isEmpty()) {
//                builder.setValue("info", def.info)
//            }
//
//            if (def.allowed.isNotEmpty()) {
//                builder.setValue("allowedValues", def.allowed)
//            } else if (def.enumeration != Any::class) {
//                if (def.enumeration.java.isEnum) {
//                    val values = def.enumeration.java.enumConstants
//                    builder.setValue("allowedValues", values.map { it.toString() })
//                } else {
//                    throw RuntimeException("Only enumeration classes are allowed in 'enumeration' annotation property")
//                }
//            }
//
//            if (def.def.isNotEmpty()) {
//                builder.setValue("default", def.def)
//            } else if (!def.required) {
//                builder.setValue("required", def.required)
//            }
//
//            if (def.tags.isNotEmpty()) {
//                builder.setValue("tags", def.tags)
//            }
//            return ValueDescriptor(builder)
//        }
//
//        /**
//         * Build empty value descriptor
//         */
//        fun empty(valueName: String): ValueDescriptor {
//            val builder = MetaBuilder("value")
//                .setValue("name", valueName)
//            return ValueDescriptor(builder)
//        }
//
//        /**
//         * Merge two separate value descriptors
//         */
//        fun merge(primary: ValueDescriptor, secondary: ValueDescriptor): ValueDescriptor {
//            return ValueDescriptor(Laminate(primary.meta, secondary.meta))
//        }
    }
}
