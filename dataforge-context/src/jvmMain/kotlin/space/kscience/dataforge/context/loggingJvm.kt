package space.kscience.dataforge.context

import org.slf4j.LoggerFactory
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.names.Name

public class SlfLogManager : AbstractPlugin(), LogManager {

    override fun logger(name: Name): Logger = Logger { level, source, body ->
        val logger = LoggerFactory.getLogger(
            if (source == null) name.toString() else "$source.$name"
        )
        val message = body.safe
        when (level) {
            Logger.Level.DEBUG -> logger.debug(message)
            Logger.Level.INFO -> logger.info(message)
            Logger.Level.WARNING -> logger.warn(message)
            Logger.Level.ERROR -> logger.error(message)
            else -> logger.trace(message)
        }
    }

    override val defaultLogger: Logger = logger(Name.EMPTY)

    override val tag: PluginTag get() = Companion.tag

    public companion object : PluginFactory<SlfLogManager> {
        override fun build(context: Context, meta: Meta): SlfLogManager = SlfLogManager()

        override val tag: PluginTag = PluginTag(group = PluginTag.DATAFORGE_GROUP, name = "log.slf4j")
    }
}

internal actual fun getGlobalLoggerFactory(): PluginFactory<out LogManager> = SlfLogManager
