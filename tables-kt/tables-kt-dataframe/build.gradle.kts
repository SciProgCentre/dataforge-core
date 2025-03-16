plugins {
    id("space.kscience.gradle.jvm")
    `maven-publish`
}

dependencies {
    api(libs.kotlinx.dataframe)
    api(projects.tablesKt)
}

readme {
    maturity = space.kscience.gradle.Maturity.PROTOTYPE
}
