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

import hep.dataforge.meta.*
import hep.dataforge.names.toName

/**
 * Descriptor for meta node. Could contain additional information for viewing
 * and editing.
 *
 * @author Alexander Nozik
 */
class NodeDescriptor(override val config: Config) : Specification {

    /**
     * The name of this node
     *
     * @return
     */
    var name: String by string { error("Anonymous descriptors are not allowed") }

    /**
     * True if multiple children with this nodes name are allowed. Anonymous
     * nodes are always single
     *
     * @return
     */
    var multiple: Boolean by boolean(false)

    /**
     * True if the node is required
     *
     * @return
     */
    var required: Boolean by boolean(false)

    /**
     * The node description
     *
     * @return
     */
    var info: String? by string()

    /**
     * A list of tags for this node. Tags used to customize node usage
     *
     * @return
     */
    var tags: List<String> by value().map { value ->
        value?.list?.map { it.string } ?: emptyList()
    }

    /**
     * The list of value descriptors
     */
    val values: Map<String, ValueDescriptor>
        get() = config.getAll("value".toName()).entries.associate { (name, node) ->
            name to ValueDescriptor.wrap(node.node ?: error("Value descriptor must be a node"))
        }

    /**
     * The map of children node descriptors
     */
    val nodes: Map<String, NodeDescriptor>
        get() = config.getAll("node".toName()).entries.associate { (name, node) ->
            name to NodeDescriptor.wrap(node.node ?: error("Node descriptor must be a node"))
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


    fun builder(): DescriptorBuilder = DescriptorBuilder(this.name, Configuration(this.meta))

    //override val descriptor: NodeDescriptor =  empty("descriptor")

    companion object : SpecificationCompanion<NodeDescriptor> {

        override fun wrap(config: Config): NodeDescriptor = NodeDescriptor(config)

        fun empty(nodeName: String): NodeDescriptor {
            return NodeDescriptor(Meta.buildEmpty(nodeName))
        }
    }
}
