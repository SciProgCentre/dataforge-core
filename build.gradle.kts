@file:OptIn(ExperimentalAbiValidation::class)

import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import space.kscience.gradle.useApache2Licence
import space.kscience.gradle.useSPCTeam

plugins {
    alias(spclibs.plugins.kscience.project)
    alias(spclibs.plugins.kotlinx.kover)
}

allprojects {
    group = "space.kscience"
    version = "0.11.0-dev-6"
}

subprojects {
    apply(plugin = "maven-publish")
}

dependencies {
    subprojects.forEach {
        dokka(it)
    }
}

readme {
    readmeTemplate = file("docs/README-TEMPLATE.md")
}


kscienceProject {
    pom("https://github.com/SciProgCentre/kmath") {
        useApache2Licence()
        useSPCTeam()
    }
    publishTo("spc", "https://maven.sciprog.center/kscience")
    publishToCentral()

    abiValidation {
        filters {
            exclude {
                annotatedWith.add("space.kscience.dataforge.misc.DFExperimental")
            }
        }
    }
}
