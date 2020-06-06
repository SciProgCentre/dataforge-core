package hep.dataforge.meta.descriptors

/**
 * An object which provides its descriptor
 */
interface Described {
    val descriptor: ItemDescriptor?

    companion object {
        const val DESCRIPTOR_NODE = "@descriptor"
    }
}

///**
// * If meta node supplies explicit descriptor, return it, otherwise try to use descriptor node from meta itself
// */
//val MetaRepr.descriptor: NodeDescriptor?
//    get() {
//        return if (this is Described) {
//            descriptor
//        } else {
//            toMeta()[DESCRIPTOR_NODE].node?.let { NodeDescriptor.wrap(it) }
//        }
//    }