package hep.dataforge.meta

/**
 * General marker for dataforge builders
 */
@DslMarker
public annotation class DFBuilder

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@Retention(AnnotationRetention.BINARY)
public annotation class DFExperimental