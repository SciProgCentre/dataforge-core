plugins {
    id("ru.mipt.npm.project")
}

val dataforgeVersion by extra("0.3.0")



allprojects {
    group = "hep.dataforge"
    version = dataforgeVersion

    apply<org.jetbrains.dokka.gradle.DokkaPlugin>()
}

subprojects {
    apply(plugin = "ru.mipt.npm.publish")
}

readme {
    readmeTemplate = file("docs/templates/README-TEMPLATE.md")
}

ksciencePublish {
    bintrayRepo = "dataforge"
    githubProject = "dataforge-core"
    spaceRepo = "https://maven.jetbrains.space/mipt-npm/p/df/maven"
}