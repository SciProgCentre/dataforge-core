import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

buildscript {
    val kotlinVersion: String by rootProject.extra("1.3.20")
    val ioVersion: String by rootProject.extra("0.1.2")
    val coroutinesVersion: String by rootProject.extra("1.1.1")
    val serializationVersion: String by rootProject.extra("0.9.1")

    repositories {
        jcenter()
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath("org.jetbrains.kotlin:kotlin-serialization:$kotlinVersion")
        classpath("org.jfrog.buildinfo:build-info-extractor-gradle:4+")
    }
}

plugins {
    id("com.jfrog.artifactory") version "4.8.1" apply false
//    id("org.jetbrains.kotlin.multiplatform") apply false
}

allprojects {
    apply(plugin = "maven-publish")
    apply(plugin = "com.jfrog.artifactory")

    repositories {
        jcenter()
        maven("https://kotlin.bintray.com/kotlinx")
    }

    group = "hep.dataforge"
    version = "0.1.1-dev-2"

    extensions.findByType<KotlinMultiplatformExtension>()?.apply {
        jvm {
            compilations.all {
                kotlinOptions {
                    jvmTarget = "1.8"
                }
            }
        }
        targets.all {
            sourceSets.all {
                languageSettings.progressiveMode = true
            }
        }
    }
}

if (file("artifactory.gradle").exists()) {
    apply(from = "artifactory.gradle")
}