package space.kscience.dataforge.context


internal actual val globalLoggerFactory: PluginFactory<out LogManager> = DefaultLogManager
