package hep.dataforge.meta

/**
 * General marker for dataforge builders
 */
@DslMarker
annotation class DFBuilder

@Experimental(level = Experimental.Level.WARNING)
@Retention(AnnotationRetention.BINARY)
annotation class DFExperimental