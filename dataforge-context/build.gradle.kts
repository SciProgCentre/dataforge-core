plugins {
    id("space.kscience.gradle.mpp")
}

description = "Context and provider definitions"

kscience {
    jvm()
    js()
    native()
    useCoroutines()
    useSerialization()
    dependencies {
        api(project(":dataforge-meta"))
    }
    dependencies(jvmMain){
        api(kotlin("reflect"))
        api("org.slf4j:slf4j-api:1.7.30")
    }
}

readme {
    maturity = space.kscience.gradle.Maturity.DEVELOPMENT
}