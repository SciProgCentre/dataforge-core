@file:OptIn(ExperimentalAbiValidation::class)

import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import space.kscience.gradle.useApache2Licence
import space.kscience.gradle.useSPCTeam

plugins {
    alias(spclibs.plugins.kscience.project)
    alias(spclibs.plugins.kotlinx.kover)
}

allprojects {
    group = "space.kscience"
    version = "0.10.4-dev-1"
}

subprojects {
    apply(plugin = "maven-publish")

    tasks.withType<KotlinCompile> {
        compilerOptions {
            freeCompilerArgs.add("-Xcontext-parameters")
        }
    }
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
            excluded {
                annotatedWith.add("space.kscience.dataforge.misc.DFExperimental")
            }
        }
    }
}
