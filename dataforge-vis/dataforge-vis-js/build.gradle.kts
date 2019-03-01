plugins{
    kotlin("js")
    id("kotlin")
}

//        configure(listOf(compilations.main, compilations.test)) {
//            tasks.getByName(compileKotlinTaskName).kotlinOptions {
//                sourceMap = true
//                moduleKind = "umd"
//                metaInfo = true
//            }
//        }
//
//        configure(compilations.main) {
//            tasks.getByName(compileKotlinTaskName).kotlinOptions {
//                main = "call"
//            }
//        }

dependencies {
    implementation("info.laht.threekt:threejs-wrapper:0.88-npm-1")
}

extensions.findByType<KotlinFrontendExtension>()?.apply {
    extensions.findByType<NpmExtension>()?.apply {
        dependency("three")
        dependency("three-orbitcontrols")
        devDependency("karma")

    }

    sourceMaps = true

    bundle("webpack") {
        this as WebPackExtension
        bundleName = "main"
        proxyUrl = "http://localhost:8080"
        contentPath = file("src/main/web")
        sourceMapEnabled = true
        mode = "development"
    }
}