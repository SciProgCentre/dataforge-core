package space.kscience.dataforge.io

import space.kscience.dataforge.context.ContextBuilder
import space.kscience.dataforge.meta.set
import space.kscience.dataforge.meta.string
import java.nio.file.Path
import kotlin.io.path.Path


public val IOPlugin.workDirectory: Path
    get() {
        val workDirectoryPath = meta[IOPlugin.WORK_DIRECTORY_KEY].string
            ?: context.properties[IOPlugin.WORK_DIRECTORY_KEY].string
            ?: ".dataforge"

        return Path(workDirectoryPath)
    }

public fun ContextBuilder.workDirectory(path: String) {
    properties {
        set(IOPlugin.WORK_DIRECTORY_KEY, path)
    }
}

public fun ContextBuilder.workDirectory(path: Path) {
    workDirectory(path.toAbsolutePath().toString())
}
