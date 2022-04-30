plugins {
    id("ru.mipt.npm.gradle.mpp")
    id("ru.mipt.npm.gradle.native")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":dataforge-context"))
                api(project(":dataforge-data"))
                api(project(":dataforge-io"))
            }
        }
        jvmMain {
            dependencies {
                // TODO include fat jar of lambdarpc
                api(files("lambdarpc-core-0.0.1.jar"))
                runtimeOnly("io.grpc:grpc-netty-shaded:1.44.0")
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
                api("io.grpc:grpc-protobuf:1.44.0")
                api("com.google.protobuf:protobuf-java-util:3.19.4")
                api("com.google.protobuf:protobuf-kotlin:3.19.4")
                api("io.grpc:grpc-kotlin-stub:1.2.1")
                api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
                api("org.slf4j:slf4j-simple:1.7.36")
                api("io.github.microutils:kotlin-logging-jvm:2.1.21")
            }
        }
    }
}

kscience {
    useSerialization {
        json()
    }
}

readme {
    maturity = ru.mipt.npm.gradle.Maturity.EXPERIMENTAL
}