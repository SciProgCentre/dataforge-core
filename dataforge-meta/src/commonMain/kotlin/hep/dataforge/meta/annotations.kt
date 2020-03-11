package hep.dataforge.meta

/**
 * General marker for dataforge builders
 */
@DslMarker
annotation class DFBuilder

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@Retention(AnnotationRetention.BINARY)
annotation class DFExperimental