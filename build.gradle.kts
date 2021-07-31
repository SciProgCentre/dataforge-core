plugins {
    id("ru.mipt.npm.gradle.project")
}

allprojects {
    group = "space.kscience"
    version = "0.5.0-dev-4"
}

subprojects {
    apply(plugin = "maven-publish")
}

readme {
    readmeTemplate = file("docs/templates/README-TEMPLATE.md")
}

ksciencePublish {
    github("dataforge-core")
    space("https://maven.pkg.jetbrains.space/mipt-npm/p/sci/maven")
    sonatype()
}

apiValidation {
    if(project.version.toString().contains("dev")) {
        validationDisabled = true
    }
    nonPublicMarkers.add("space.kscience.dataforge.misc.DFExperimental")
}