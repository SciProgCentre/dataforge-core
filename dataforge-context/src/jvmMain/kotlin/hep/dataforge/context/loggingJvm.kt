package hep.dataforge.context

import mu.KLogger
import mu.KotlinLogging

public actual typealias Logger = KLogger

public actual fun Context.buildLogger(name: String): Logger = KotlinLogging.logger(name)