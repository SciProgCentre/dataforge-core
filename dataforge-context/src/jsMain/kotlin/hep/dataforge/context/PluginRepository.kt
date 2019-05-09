package hep.dataforge.context


actual object PluginRepository {

    private val factories: MutableSet<PluginFactory<*>> = HashSet()

    actual fun register(factory: PluginFactory<*>) {
        factories.add(factory)
    }

    /**
     * List plugins available in the repository
     */
    actual fun list(): Sequence<PluginFactory<*>> = factories.asSequence()
}