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
import hep.dataforge.names.NameToken
import hep.dataforge.names.toName

/**
 * Descriptor for meta node. Could contain additional information for viewing
 * and editing.
 *
 * @author Alexander Nozik
 */
class NodeDescriptor(override val config: Config) : Specific {

    /**
     * The name of this node
     *
     * @return
     */
    var name: String by string { error("Anonymous descriptors are not allowed") }


    /**
     * The default for this node. Null if there is no default.
     *
     * @return
     */
    var default: Meta? by node()

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
    var required: Boolean by boolean { default == null }

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

    fun value(name: String, descriptor: ValueDescriptor) {
        val token = NameToken("value", name)
        config[token] = descriptor.config
    }

    /**
     * Add a value descriptor using block for
     */
    fun value(name: String, block: ValueDescriptor.() -> Unit) {
        value(name, ValueDescriptor.build { this.name = name }.apply(block))
    }

    /**
     * The map of children node descriptors
     */
    val nodes: Map<String, NodeDescriptor>
        get() = config.getAll("node".toName()).entries.associate { (name, node) ->
            name to NodeDescriptor.wrap(node.node ?: error("Node descriptor must be a node"))
        }


    fun node(name: String, descriptor: NodeDescriptor) {
        val token = NameToken("node", name)
        config[token] = descriptor.config
    }

    fun node(name: String, block: NodeDescriptor.() -> Unit) {
        node(name, NodeDescriptor.build { this.name = name }.apply(block))
    }


    //override val descriptor: NodeDescriptor =  empty("descriptor")

    companion object : Specification<NodeDescriptor> {

        override fun wrap(config: Config): NodeDescriptor = NodeDescriptor(config)

    }
}
