package hep.dataforge.context


public actual interface Logger {
    /**
     * Lazy add a log message if isTraceEnabled is true
     */
    public actual fun trace(msg: () -> Any?)

    /**
     * Lazy add a log message if isDebugEnabled is true
     */
    public actual fun debug(msg: () -> Any?)

    /**
     * Lazy add a log message if isInfoEnabled is true
     */
    public actual fun info(msg: () -> Any?)

    /**
     * Lazy add a log message if isWarnEnabled is true
     */
    public actual fun warn(msg: () -> Any?)

    /**
     * Lazy add a log message if isErrorEnabled is true
     */
    public actual fun error(msg: () -> Any?)

}

public actual fun Context.buildLogger(name: String): Logger = object :Logger{
    override fun trace(msg: () -> Any?) {
        println("[TRACE] $name - ${msg()}")
    }

    override fun debug(msg: () -> Any?) {
        println("[DEBUG] $name - ${msg()}")
    }

    override fun info(msg: () -> Any?) {
        println("[INFO] $name - ${msg()}")
    }

    override fun warn(msg: () -> Any?) {
        println("[WARNING] $name - ${msg()}")
    }

    override fun error(msg: () -> Any?) {
        println("[ERROR] $name - ${msg()}")
    }

}