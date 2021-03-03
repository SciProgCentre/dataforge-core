plugins {
    id("ru.mipt.npm.gradle.project")
}

allprojects {
    group = "space.kscience"
    version = "0.4.0-dev-1"

    apply<org.jetbrains.dokka.gradle.DokkaPlugin>()
}

subprojects {
    apply(plugin = "ru.mipt.npm.gradle.publish")
    repositories{
        maven("https://dl.bintray.com/mipt-npm/kscience")
        maven("https://dl.bintray.com/mipt-npm/dev")
    }
}

readme {
    readmeTemplate = file("docs/templates/README-TEMPLATE.md")
}

ksciencePublish {
    bintrayRepo = "dataforge"
    githubProject = "dataforge-core"
    spaceRepo = "https://maven.jetbrains.space/mipt-npm/p/df/maven"
}

apiValidation{
    nonPublicMarkers.add("space.kscience.dataforge.misc.DFExperimental")
}