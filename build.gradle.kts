import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import space.kscience.gradle.isInDevelopment
import space.kscience.gradle.useApache2Licence
import space.kscience.gradle.useSPCTeam

plugins {
    id("space.kscience.gradle.project")
}

allprojects {
    group = "space.kscience"
    version = "0.6.2-dev-3"
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
    github("dataforge-core", "SciProgCentre")
    space(
        if (isInDevelopment) {
            "https://maven.pkg.jetbrains.space/spc/p/sci/dev"
        } else {
            "https://maven.pkg.jetbrains.space/spc/p/sci/maven"
        }
    )
    sonatype()
}

apiValidation {
    nonPublicMarkers.add("space.kscience.dataforge.misc.DFExperimental")
}