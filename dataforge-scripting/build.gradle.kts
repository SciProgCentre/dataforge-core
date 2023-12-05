plugins {
    id("space.kscience.gradle.mpp")
}

kscience{
    jvm()
    commonMain {
        api(projects.dataforgeWorkspace)
        implementation(kotlin("scripting-common"))
    }
    jvmMain{
        implementation(kotlin("scripting-jvm-host"))
        implementation(kotlin("scripting-jvm"))
    }
    jvmTest{
        implementation(spclibs.logback.classic)
    }
}


readme{
    maturity = space.kscience.gradle.Maturity.PROTOTYPE
}