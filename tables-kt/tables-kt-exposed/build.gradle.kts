plugins {
    id("space.kscience.gradle.jvm")
    `maven-publish`
}

dependencies {
    api(projects.tablesKt)
    api(libs.exposed.core)
    testImplementation(libs.exposed.jdbc)
    testImplementation("com.h2database:h2:2.4.240")
    testImplementation(spclibs.logback.classic)
}

readme {
    maturity = space.kscience.gradle.Maturity.EXPERIMENTAL
}
