package hep.dataforge.context

import hep.dataforge.meta.Meta
import hep.dataforge.names.Name
import mu.KotlinLogging
import kotlin.reflect.KClass

public class KLoggingManager : AbstractPlugin(), LogManager {

    override fun log(name: Name, tag: String, body: () -> String) {
        val logger = KotlinLogging.logger("[${context.name}] $name")
        when (tag) {
            LogManager.DEBUG -> logger.debug(body)
            LogManager.INFO -> logger.info(body)
            LogManager.WARNING -> logger.warn(body)
            LogManager.ERROR -> logger.error(body)
            else -> logger.trace(body)
        }
    }

    override val tag: PluginTag get() = Companion.tag

    public companion object : PluginFactory<KLoggingManager> {
        override fun invoke(meta: Meta, context: Context): KLoggingManager = KLoggingManager()

        override val tag: PluginTag = PluginTag(group = PluginTag.DATAFORGE_GROUP, name = "log.kotlinLogging")
        override val type: KClass<out KLoggingManager> = KLoggingManager::class
    }
}

internal actual val globalLogger: LogManager = KLoggingManager().apply { attach(Global) }
