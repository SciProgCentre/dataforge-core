plugins {
    id("space.kscience.gradle.mpp")
    id("space.kscience.gradle.native")
}

kscience {
    useSerialization{
        json()
    }
}

description = "Meta definition and basic operations on meta"

readme{
    maturity = space.kscience.gradle.Maturity.DEVELOPMENT
}