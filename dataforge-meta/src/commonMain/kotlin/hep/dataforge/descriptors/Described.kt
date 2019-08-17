package hep.dataforge.descriptors

import hep.dataforge.descriptors.Described.Companion.DESCRIPTOR_NODE
import hep.dataforge.meta.Meta
import hep.dataforge.meta.get
import hep.dataforge.meta.node

/**
 * An object which provides its descriptor
 */
interface Described {
    val descriptor: NodeDescriptor

    companion object {
        const val DESCRIPTOR_NODE = "@descriptor"
    }
}

/**
 * If meta node supplies explicit descriptor, return it, otherwise try to use descriptor node from meta itself
 */
val Meta.descriptor: NodeDescriptor?
    get() {
        return if (this is Described) {
            descriptor
        } else {
            get(DESCRIPTOR_NODE).node?.let { NodeDescriptor.wrap(it) }
        }
    }