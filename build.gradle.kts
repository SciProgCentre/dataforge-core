val dataforgeVersion by extra("0.1.3-dev-6")

allprojects {
    repositories {
        jcenter()
        maven("https://kotlin.bintray.com/kotlinx")
    }

    group = "hep.dataforge"
    version = dataforgeVersion
}

subprojects {
    if (name.startsWith("dataforge")) {
        apply(plugin = "scientifik.publish")
    } 
}