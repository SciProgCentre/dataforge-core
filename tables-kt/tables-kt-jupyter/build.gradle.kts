plugins {
    id("space.kscience.gradle.jvm")
    alias(spclibs.plugins.kotlin.jupyter.api)
    `maven-publish`
}

dependencies {
    api(projects.tablesKt)
    api(spclibs.kotlinx.html)
}

kotlinJupyter{
    integrations {
        producer("space.kscience.tables.TablesForJupyter")
    }
}

readme {
    maturity = space.kscience.gradle.Maturity.EXPERIMENTAL
}
