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

import hep.dataforge.meta.DFExperimental
import hep.dataforge.values.ValueType
import kotlin.reflect.KClass

@MustBeDocumented
annotation class Attribute(
    val key: String,
    val value: String
)

@MustBeDocumented
annotation class Attributes(
    val attrs: Array<Attribute>
)

@MustBeDocumented
annotation class ItemDef(
    val info: String = "",
    val multiple: Boolean = false,
    val required: Boolean = false
)

@Target(AnnotationTarget.PROPERTY)
@MustBeDocumented
annotation class ValueDef(
    val type: Array<ValueType> = [ValueType.STRING],
    val def: String = "",
    val allowed: Array<String> = [],
    val enumeration: KClass<*> = Any::class
)

///**
// * Description text for meta property, node or whole object
// */
//@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
//@Retention(AnnotationRetention.RUNTIME)
//@MustBeDocumented
//annotation class Description(val value: String)
//
///**
// * Annotation for value property which states that lists are expected
// */
//@Target(AnnotationTarget.PROPERTY)
//@Retention(AnnotationRetention.RUNTIME)
//@MustBeDocumented
//annotation class Multiple
//
///**
// * Descriptor target
// * The DataForge path to the resource containing the description. Following targets are supported:
// *  1. resource
// *  1. file
// *  1. class
// *  1. method
// *  1. property
// *
// *
// * Does not work if [type] is provided
// */
//@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
//@Retention(AnnotationRetention.RUNTIME)
//@MustBeDocumented
//annotation class Descriptor(val value: String)
//
//
///**
// * Aggregator class for descriptor nodes
// */
//@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
//@Retention(AnnotationRetention.RUNTIME)
//@MustBeDocumented
//annotation class DescriptorNodes(vararg val nodes: NodeDef)
//
///**
// * Aggregator class for descriptor values
// */
//@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
//@Retention(AnnotationRetention.RUNTIME)
//@MustBeDocumented
//annotation class DescriptorValues(vararg val nodes: ValueDef)
//
///**
// * Alternative name for property descriptor declaration
// */
//@Target(AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
//@Retention(AnnotationRetention.RUNTIME)
//@MustBeDocumented
//annotation class DescriptorName(val name: String)
//
//@Target(AnnotationTarget.PROPERTY)
//@Retention(AnnotationRetention.RUNTIME)
//@MustBeDocumented
//annotation class DescriptorValue(val def: ValueDef)
////TODO enter fields directly?
//
//@Target(AnnotationTarget.PROPERTY)
//@Retention(AnnotationRetention.RUNTIME)
//@MustBeDocumented
//annotation class ValueProperty(
//    val name: String = "",
//    val type: Array<ValueType> = arrayOf(ValueType.STRING),
//    val multiple: Boolean = false,
//    val def: String = "",
//    val enumeration: KClass<*> = Any::class,
//    val tags: Array<String> = emptyArray()
//)
//
//
//@Target(AnnotationTarget.PROPERTY)
//@Retention(AnnotationRetention.RUNTIME)
//@MustBeDocumented
//annotation class NodeProperty(val name: String = "")
