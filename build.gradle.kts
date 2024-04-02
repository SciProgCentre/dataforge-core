import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import space.kscience.gradle.useApache2Licence
import space.kscience.gradle.useSPCTeam

plugins {
    id("space.kscience.gradle.project")
}

allprojects {
    group = "space.kscience"
    version = "0.8.1"
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
    repository("spc", "https://maven.sciprog.center/kscience")
    sonatype("https://oss.sonatype.org")
}

apiValidation {
    nonPublicMarkers.add("space.kscience.dataforge.misc.DFExperimental")
}