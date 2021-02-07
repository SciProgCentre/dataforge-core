plugins {
    id("ru.mipt.npm.mpp")
    id("ru.mipt.npm.native")
}

kscience {
    useSerialization{
        json()
    }
}

description = "Meta definition and basic operations on meta"

readme{
    maturity = ru.mipt.npm.gradle.Maturity.DEVELOPMENT
}