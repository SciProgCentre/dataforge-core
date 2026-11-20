package space.kscience.dataforge.context

import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.names.Name

public class ConsoleLogManager : AbstractPlugin(), LogManager {

    override fun logger(name: Name): Logger  = Logger { level, source, body ->
        val message: String = body.safe

        val prefix = if (source == null) "" else "[$source] "
        when (level) {
            // TODO depends on https://youtrack.jetbrains.com/issue/KT-33595/
            Logger.Level.DEBUG -> console.asDynamic().debug("$prefix$name: $message")
            Logger.Level.INFO -> console.info("$prefix$name: $message")
            Logger.Level.WARNING ->  console.warn("$prefix$name: $message")
            Logger.Level.ERROR -> console.error("$prefix$name: $message")
            else ->  console.log("$prefix$name: $message")
        }
    }

    override val defaultLogger: Logger = logger(Name.EMPTY)


    override val tag: PluginTag get() = Companion.tag

    public companion object : PluginFactory<ConsoleLogManager> {
        override fun build(context: Context, meta: Meta): ConsoleLogManager = ConsoleLogManager()

        override val tag: PluginTag = PluginTag(group = PluginTag.DATAFORGE_GROUP, name = "log.jsConsole")
    }
}

internal actual fun getGlobalLoggerFactory(): PluginFactory<out LogManager> = ConsoleLogManager
