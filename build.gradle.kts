import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("space.kscience.gradle.project")
}

allprojects {
    group = "space.kscience"
    version = "0.6.0-dev-14"
}

subprojects {
    apply(plugin = "maven-publish")

    tasks.withType<KotlinCompile>{
        kotlinOptions{
            freeCompilerArgs = freeCompilerArgs + "-Xcontext-receivers"
        }
    }
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
    nonPublicMarkers.add("space.kscience.dataforge.misc.DFExperimental")
}