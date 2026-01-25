plugins {
    id("space.kscience.gradle.jvm")
    `maven-publish`
}

description = "An integration to represent SQL tables as DataForg table via Exposed-JDBC"

dependencies {
    api(projects.tablesKt)
    api(libs.exposed.jdbc)
    testImplementation("com.h2database:h2:2.4.240")
    testImplementation(spclibs.logback.classic)
}

readme {
    maturity = space.kscience.gradle.Maturity.EXPERIMENTAL
    
    feature("ExposedTable", "src/main/kotlin/space/kscience/dataforge/exposed/ExposedTable.kt"){
        "Create a table from JDBC database with `ExposedTable`."
    }
}
