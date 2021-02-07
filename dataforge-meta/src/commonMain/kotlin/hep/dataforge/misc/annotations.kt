package hep.dataforge.misc

/**
 * General marker for dataforge builders
 */
@DslMarker
public annotation class DFBuilder

/**
 * The declaration is experimental and could be changed in future
 */
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@Retention(AnnotationRetention.BINARY)
public annotation class DFExperimental

/**
 * The declaration is internal to the DataForge and could use unsafe or unstable features.
 */
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@Retention(AnnotationRetention.BINARY)
public annotation class DFInternal