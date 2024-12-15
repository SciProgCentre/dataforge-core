plugins {
    id("space.kscience.gradle.mpp")
}

kscience {
    jvm()
    js()
    native()
    wasm()
    useSerialization{
        json()
    }
}

description = "Meta definition and basic operations on meta"

readme{
    maturity = space.kscience.gradle.Maturity.DEVELOPMENT

    feature("metadata"){
        """
            
        """.trimIndent()
    }
}