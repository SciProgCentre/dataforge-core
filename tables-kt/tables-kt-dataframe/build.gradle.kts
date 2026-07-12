plugins {
    id("space.kscience.gradle.jvm")
    `maven-publish`
}

description = "An integration to convert DataForg tables to DataFrame and back."

dependencies {
    api(libs.kotlinx.dataframe)
    api(projects.tablesKt)
}

readme {
    maturity = space.kscience.gradle.Maturity.EXPERIMENTAL
    
    feature("asDataFrame", "src/main/kotlin/space/kscience/dataforge/dataframe/TableAsDataFrame.kt"){
        "Conversion from DataForge Table to DataFrame via `asDataFrame()` extension function."
    }

    feature("asTable", "src/main/kotlin/space/kscience/dataforge/dataframe/DataFrameAsTable.kt"){
        "Conversion from DataFrame to DataForge Table via `asTable()` extension function."
    }
}
