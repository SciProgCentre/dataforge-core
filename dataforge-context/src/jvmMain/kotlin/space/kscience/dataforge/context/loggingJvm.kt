package space.kscience.dataforge.context

import org.slf4j.LoggerFactory
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.names.Name
import kotlin.reflect.KClass

public class SlfLogManager : AbstractPlugin(), LogManager {

    override fun log(name: Name, tag: String, body: () -> String) {
        val logger = LoggerFactory.getLogger("[${context.name}] $name") //KotlinLogging.logger("[${context.name}] $name")
        val message = body.safe
        when (tag) {
            LogManager.DEBUG -> logger.debug(message)
            LogManager.INFO -> logger.info(message)
            LogManager.WARNING -> logger.warn(message)
            LogManager.ERROR -> logger.error(message)
            else -> logger.trace(message)
        }
    }

    override val tag: PluginTag get() = Companion.tag

    public companion object : PluginFactory<SlfLogManager> {
        override fun invoke(meta: Meta, context: Context): SlfLogManager = SlfLogManager()

        override val tag: PluginTag = PluginTag(group = PluginTag.DATAFORGE_GROUP, name = "log.kotlinLogging")
        override val type: KClass<out SlfLogManager> = SlfLogManager::class
    }
}

internal actual val globalLoggerFactory: PluginFactory<out LogManager> = SlfLogManager
