plugins {
    id("ru.mipt.npm.gradle.jvm")
}

dependencies {
    api("org.jetbrains.exposed:exposed-core:0.31.1")
    testImplementation("org.jetbrains.exposed:exposed-jdbc:0.31.1")
    testImplementation("com.h2database:h2:1.4.200")
    testImplementation("org.slf4j:slf4j-simple:1.7.30")
    api(project(":dataforge-tables"))
}

readme {
    maturity = ru.mipt.npm.gradle.Maturity.PROTOTYPE
}
