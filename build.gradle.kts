plugins {
    id("ru.mipt.npm.project")
}

val dataforgeVersion by extra("0.3.0-dev")

val bintrayRepo by extra("dataforge")
val githubProject by extra("dataforge-core")
val spaceRepo by extra("https://maven.jetbrains.space/mipt-npm/p/df/maven")

allprojects {
    group = "hep.dataforge"
    version = dataforgeVersion

    apply<org.jetbrains.dokka.gradle.DokkaPlugin>()

    repositories {
        mavenLocal()
    }
}

apiValidation{
    validationDisabled = true
}

subprojects {
    apply(plugin = "ru.mipt.npm.publish")
}

apiValidation{
    ignoredProjects.add("dataforge-tables")
}