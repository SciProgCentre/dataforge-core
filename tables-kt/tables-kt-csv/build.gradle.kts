plugins {
    id("space.kscience.gradle.mpp")
    `maven-publish`
}

kscience {
    jvm()
    js()
    commonMain {
        api(projects.tablesKt)
        api(libs.csv)
    }
}

readme {
    maturity = space.kscience.gradle.Maturity.EXPERIMENTAL
}