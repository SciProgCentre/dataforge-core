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
package hep.dataforge.descriptors

import hep.dataforge.Named
import hep.dataforge.description.ValueDescriptor
import hep.dataforge.meta.*
import hep.dataforge.names.Name
import java.util.*

/**
 * Descriptor for meta node. Could contain additional information for viewing
 * and editing.
 *
 * @author Alexander Nozik
 */
open class NodeDescriptor(val meta: Meta) : MetaRepr {

    /**
     * True if multiple children with this nodes name are allowed. Anonymous
     * nodes are always single
     *
     * @return
     */
    val multiple: Boolean by meta.boolean(false)

    /**
     * True if the node is required
     *
     * @return
     */
    val required: Boolean by meta.boolean(false)

    /**
     * The node description
     *
     * @return
     */
    open val info: String by meta.string("")

    /**
     * A list of tags for this node. Tags used to customize node usage
     *
     * @return
     */
    val tags: List<String> by customValue(def = emptyList()) { it.list.map { it.string } }

    /**
     * The name of this node
     *
     * @return
     */
    override val name: String by stringValue(def = meta.name)

    /**
     * The list of value descriptors
     *
     * @return
     */
    fun valueDescriptors(): Map<String, ValueDescriptor> {
        val map = HashMap<String, ValueDescriptor>()
        if (meta.hasMeta("value")) {
            for (valueNode in meta.getMetaList("value")) {
                val vd = ValueDescriptor(valueNode)
                map[vd.name] = vd
            }
        }
        return map
    }

    /**
     * The child node descriptor for given name. Name syntax is supported.
     *
     * @param name
     * @return
     */
    fun getNodeDescriptor(name: String): NodeDescriptor? {
        return getNodeDescriptor(Name.of(name))
    }

    fun getNodeDescriptor(name: Name): NodeDescriptor? {
        return if (name.length == 1) {
            childrenDescriptors()[name.unescaped]
        } else {
            getNodeDescriptor(name.cutLast())?.getNodeDescriptor(name.last)
        }
    }

    /**
     * The value descriptor for given value name. Name syntax is supported.
     *
     * @param name
     * @return
     */
    fun getValueDescriptor(name: String): ValueDescriptor? {
        return getValueDescriptor(Name.of(name))
    }

    fun getValueDescriptor(name: Name): ValueDescriptor? {
        return if (name.length == 1) {
            valueDescriptors()[name.unescaped]
        } else {
            getNodeDescriptor(name.cutLast())?.getValueDescriptor(name.last)
        }
    }

    /**
     * The map of children node descriptors
     *
     * @return
     */
    fun childrenDescriptors(): Map<String, NodeDescriptor> {
        val map = HashMap<String, NodeDescriptor>()
        if (meta.hasMeta("node")) {
            for (node in meta.getMetaList("node")) {
                val nd = NodeDescriptor(node)
                map[nd.name] = nd
            }
        }
        return map
    }

    /**
     * Check if this node has default
     *
     * @return
     */
    fun hasDefault(): Boolean {
        return meta.hasMeta("default")
    }

    /**
     * The default meta for this node (could be multiple). Null if not defined
     *
     * @return
     */
    val default: List<Meta> by nodeList(def = emptyList())

    /**
     * Identify if this descriptor has child value descriptor with default
     *
     * @param name
     * @return
     */
    fun hasDefaultForValue(name: String): Boolean {
        return getValueDescriptor(name)?.hasDefault() ?: false
    }

    /**
     * The key of the value which is used to display this node in case it is
     * multiple. By default, the key is empty which means that node index is
     * used.
     *
     * @return
     */
    val key: String by stringValue(def = "")

    override fun toMeta(): Meta {
        return meta
    }

    fun builder(): DescriptorBuilder = DescriptorBuilder(this.name, Configuration(this.meta))

    //override val descriptor: NodeDescriptor =  empty("descriptor")

    companion object {

        fun empty(nodeName: String): NodeDescriptor {
            return NodeDescriptor(Meta.buildEmpty(nodeName))
        }
    }
}
