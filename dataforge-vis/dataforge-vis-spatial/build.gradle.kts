plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    js {
        configure(listOf(compilations["main"], compilations["test"])) {
            tasks.getByName(compileKotlinTaskName) {
                kotlinOptions {
                    metaInfo = true
                    sourceMap = true
                    sourceMapEmbedSources = "always"
                    moduleKind = "umd"
                }
            }
        }

        configure(listOf(compilations["main"])) {
            tasks.getByName(compileKotlinTaskName) {
                kotlinOptions {
                    main = "call"
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":dataforge-vis"))
            }
        }
        val jvmMain by getting {
            dependencies {

            }
        }
        val jsMain by getting {
            dependencies {

            }
        }
    }
}

