package space.kscience.dataforge.misc

/**
 * A text label for internal DataForge type classification. Alternative for mime container type.
 *
 * The DataForge type notation presumes that type `A.B.C` is the subtype of `A.B`
 */
@MustBeDocumented
@Target(AnnotationTarget.CLASS)
public annotation class Type(val id: String)
