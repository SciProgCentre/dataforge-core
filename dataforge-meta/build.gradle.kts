plugins {
    id("space.kscience.gradle.mpp")
}

kscience {
    jvm()
    js()
    native()
    useSerialization{
        json()
    }
}

description = "Meta definition and basic operations on meta"

readme{
    maturity = space.kscience.gradle.Maturity.DEVELOPMENT
}