package hep.dataforge.context

actual object PluginRepository {
    /**
     * List plugins available in the repository
     */
    actual fun list(): Sequence<PluginFactory> = Global.services(PluginFactory::class)

}