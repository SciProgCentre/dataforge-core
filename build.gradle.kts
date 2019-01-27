buildscript {
    extra["kotlinVersion"] = "1.3.20"
    extra["ioVersion"] = "0.1.2"
    extra["serializationVersion"] = "0.9.1"
    extra["coroutinesVersion"] = "1.1.1"

    val kotlinVersion: String by extra
    val ioVersion: String by extra
    val coroutinesVersion: String by extra
    val serializationVersion: String by extra

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
}

if (file("artifactory.gradle").exists()) {
    apply(from = "artifactory.gradle")
}