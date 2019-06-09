package hep.dataforge.descriptors

interface ItemDescriptor {
    val name: String
    val multiple: Boolean
    val required: Boolean
    val info: String?
    val tags: List<String>
}