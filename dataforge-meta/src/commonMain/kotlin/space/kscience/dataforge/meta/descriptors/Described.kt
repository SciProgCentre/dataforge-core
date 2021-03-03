package space.kscience.dataforge.meta.descriptors

/**
 * An object which provides its descriptor
 */
public interface Described {
    public val descriptor: ItemDescriptor?

    public companion object {
        //public const val DESCRIPTOR_NODE: String = "@descriptor"
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