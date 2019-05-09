import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.dokka")
    `maven-publish`
}

kotlin {

    val dokka by tasks.getting(DokkaTask::class) {
        outputFormat = "html"
        outputDirectory = "$buildDir/javadoc"
        jdkVersion = 8

        kotlinTasks {
            // dokka fails to retrieve sources from MPP-tasks so we only define the jvm task
            listOf(tasks.getByPath("compileKotlinJvm"))
        }
        sourceRoot {
            // assuming only single source dir
            path = sourceSets["commonMain"].kotlin.srcDirs.first().toString()
            platforms = listOf("Common")
        }
        // although the JVM sources are now taken from the task,
        // we still define the jvm source root to get the JVM marker in the generated html
        sourceRoot {
            // assuming only single source dir
            path = sourceSets["jvmMain"].kotlin.srcDirs.first().toString()
            platforms = listOf("JVM")
        }
    }

    val javadocJar by tasks.registering(Jar::class) {
        dependsOn(dokka)
        archiveClassifier.set("javadoc")
        from("$buildDir/javadoc")
    }

    publishing {

        //        publications.filterIsInstance<MavenPublication>().forEach { publication ->
//            if (publication.name == "kotlinMultiplatform") {
//                // for our root metadata publication, set artifactId with a package and project name
//                publication.artifactId = project.name
//            } else {
//                // for targets, set artifactId with a package, project name and target name (e.g. iosX64)
//                publication.artifactId = "${project.name}-${publication.name}"
//            }
//        }

        targets.all {
            val publication = publications.findByName(name) as MavenPublication

            // Patch publications with fake javadoc
            publication.artifact(javadocJar.get())
        }
    }
}