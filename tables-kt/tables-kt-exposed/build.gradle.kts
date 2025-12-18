plugins {
    id("space.kscience.gradle.jvm")
    `maven-publish`
}

val exposedVersion = "0.47.0"

dependencies {
    api(projects.tablesKt)
    api(libs.exposed.core)
    testImplementation(libs.exposed.jdbc)
    testImplementation("com.h2database:h2:2.3.232")
    testImplementation(spclibs.logback.classic)
}

readme {
    maturity = space.kscience.gradle.Maturity.EXPERIMENTAL
}
