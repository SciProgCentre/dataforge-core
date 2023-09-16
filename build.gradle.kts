import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import space.kscience.gradle.useApache2Licence
import space.kscience.gradle.useSPCTeam

plugins {
    id("space.kscience.gradle.project")
}

allprojects {
    group = "space.kscience"
    version = "0.6.3-dev-kotlin-1.9.20"
}

subprojects {
    apply(plugin = "maven-publish")

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = freeCompilerArgs + "-Xcontext-receivers"
        }
    }
}

readme {
    readmeTemplate = file("docs/templates/README-TEMPLATE.md")
}

ksciencePublish {
    pom("https://github.com/SciProgCentre/kmath") {
        useApache2Licence()
        useSPCTeam()
    }
    repository("spc","https://maven.sciprog.center/kscience")
    sonatype()
}

apiValidation {
    nonPublicMarkers.add("space.kscience.dataforge.misc.DFExperimental")
}