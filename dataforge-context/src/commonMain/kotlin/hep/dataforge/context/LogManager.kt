package hep.dataforge.context

import hep.dataforge.names.Name

public interface LogManager : Plugin {

    public fun log(name: Name, tag: String, body: () -> String)

    public companion object {
        public const val TRACE: String = "TRACE"
        public const val INFO: String = "INFO"
        public const val DEBUG: String = "DEBUG"
        public const val WARNING: String = "WARNING"
        public const val ERROR: String = "ERROR"
    }
}

public fun LogManager.info(name: Name = Name.EMPTY, body: () -> String): Unit = log(name,LogManager.INFO,body)

public val Context.logger: LogManager
    get() = plugins.find(inherit = true) { it is LogManager } as? LogManager ?: Global.logger


///**
// * The logger specific to this context
// */
//public val Context.logger: Logger get() = buildLogger(name.toString())
//
///**
// * The logger
// */
//public val ContextAware.logger: Logger
//    get() = if (this is Named) {
//        context.buildLogger(Path(context.name, this.name).toString())
//    } else {
//        context.logger
//    }

