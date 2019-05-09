val dataforgeVersion by extra("0.1.2")

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
        apply(plugin = "npm-publish")
    } 
}