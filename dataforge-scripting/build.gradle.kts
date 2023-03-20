plugins {
    id("space.kscience.gradle.mpp")
}

kscience{
    jvm()
    dependencies {
        api(projects.dataforgeWorkspace)
        implementation(kotlin("scripting-common"))
    }
    dependencies(jvmMain){
        implementation(kotlin("scripting-jvm-host"))
        implementation(kotlin("scripting-jvm"))
    }
    dependencies(jvmTest){
        implementation(spclibs.logback.classic)
    }
}


readme{
    maturity = space.kscience.gradle.Maturity.PROTOTYPE
}