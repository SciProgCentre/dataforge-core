import ru.mipt.npm.gradle.KScienceVersions

plugins {
    id("ru.mipt.npm.mpp")
    id("ru.mipt.npm.native")
}

kscience {
    useSerialization()
}

description = "Meta definition and basic operations on meta"

dependencies{
    commonMainApi("org.jetbrains.kotlinx:kotlinx-serialization-json:${KScienceVersions.serializationVersion}")
}