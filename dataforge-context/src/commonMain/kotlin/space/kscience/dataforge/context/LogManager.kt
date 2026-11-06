package space.kscience.dataforge.context

import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.misc.Named
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.toStringUnescaped
import space.kscience.dataforge.names.withIndex

/**
 * Receives log records produced by a context or framework component.
 *
 * A logger is a functional interface whose single operation records a message
 * with a severity [Level], an optional textual source, and a lazily evaluated
 * message body. Implementations define how records are filtered, formatted,
 * stored, or forwarded.
 *
 * The message body is supplied as a function so that implementations can avoid
 * constructing messages that will not be emitted.
 */
public fun interface Logger {
    public enum class Level {
        TRACE,
        INFO,
        DEBUG,
        WARNING,
        ERROR
    }

    public fun log(level: Level, source: String?, body: () -> String)
}

public fun Logger.log(level: Logger.Level, body: () -> String): Unit = log(level, null, body)

/**
 * Manages logging facilities for a context plugin and provides access to named loggers.
 *
 * A log manager is both a [Plugin] and a [Logger]. As a logger, it delegates records to
 * the default logger. As a manager, it resolves loggers by [Name] and allows records to
 * be written either through the default logger or through a named logger.
 *
 * Log messages are supplied lazily through the message body function, allowing logger
 * implementations to skip message construction when a record is filtered out.
 */
public interface LogManager : Plugin, Logger {
    public fun logger(name: Name): Logger

    public val defaultLogger: Logger

    override fun log(
        level: Logger.Level,
        source: String?,
        body: () -> String
    ): Unit = defaultLogger.log(level, source, body)

    public fun log(
        level: Logger.Level,
        body: () -> String
    ): Unit = defaultLogger.log(level, null, body)

    public fun log(
        name: Name,
        level: Logger.Level,
        source: String? = null,
        body: () -> String
    ): Unit = logger(name).log(level, source, body)

    public companion object {
        public const val TRACE: String = "TRACE"
        public const val INFO: String = "INFO"
        public const val DEBUG: String = "DEBUG"
        public const val WARNING: String = "WARNING"
        public const val ERROR: String = "ERROR"
    }
}

public fun Logger.trace(source: String? = null, body: () -> String): Unit = log(Logger.Level.TRACE, source, body)
public fun Logger.info(source: String? = null, body: () -> String): Unit = log(Logger.Level.INFO, source, body)
public fun Logger.debug(source: String? = null, body: () -> String): Unit = log(Logger.Level.DEBUG, source, body)
public fun Logger.warn(source: String? = null, body: () -> String): Unit = log(Logger.Level.WARNING, source, body)
public fun Logger.error(source: String? = null, body: () -> String): Unit = log(Logger.Level.ERROR, source, body)

internal val (() -> String).safe: String
    get() = try {
        invoke()
    } catch (t: Throwable) {
        "Error while evaluating log string: ${t.message}"
    }


public fun Logger.error(throwable: Throwable?, body: () -> String): Unit = log(Logger.Level.ERROR) {
    buildString {
        appendLine(body())
        throwable?.let { appendLine(throwable.stackTraceToString()) }
    }
}


public class DefaultLogManager : AbstractPlugin(), LogManager {

    override fun logger(name: Name): Logger = Logger { tag, source, body ->
        val message: String = body.safe
        println("$tag $name: [${source ?: context.name}] $message")
    }

    override val defaultLogger: Logger = logger(Name.EMPTY)


    override val tag: PluginTag get() = Companion.tag

    public companion object : PluginFactory<DefaultLogManager> {
        override fun build(context: Context, meta: Meta): DefaultLogManager = DefaultLogManager()

        override val tag: PluginTag = PluginTag(group = PluginTag.DATAFORGE_GROUP, name = "log.default")
    }
}

/**
 * Context log manager inherited from parent
 */
public val Context.logger: LogManager
    get() = plugins.find(inherit = true) { it is LogManager } as? LogManager
        ?: getGlobalLoggerFactory().build(context = Global, meta = Meta.EMPTY).apply { attach(Global) }

/**
 * The named proxy logger for a context member
 */
public val ContextAware.logger: Logger
    get() = if (this is Named) {
        Logger { level, source, body ->
            if (source != null) {
                context.logger.log(level, this.name.withIndex(source).toStringUnescaped(), body)
            } else {
                context.logger.log(level, this.name.toStringUnescaped(), body)
            }
        }
    } else {
        context.logger
    }

