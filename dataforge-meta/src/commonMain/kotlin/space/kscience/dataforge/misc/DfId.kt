package space.kscience.dataforge.misc

/**
 * A text label for internal DataForge type classification. Alternative for mime container type.
 */
@MustBeDocumented
@Target(AnnotationTarget.CLASS)
public annotation class DfId(val id: String)
