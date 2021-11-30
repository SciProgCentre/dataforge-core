package space.kscience.dataforge.context


internal actual fun getGlobalLoggerFactory(): PluginFactory<out LogManager> = DefaultLogManager
