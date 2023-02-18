plugins {
    id("space.kscience.gradle.mpp")
}

kscience {
    jvm()
    js()
    native()
    useSerialization("1.4.1"){
        json()
    }
}

description = "Meta definition and basic operations on meta"

readme{
    maturity = space.kscience.gradle.Maturity.DEVELOPMENT
}