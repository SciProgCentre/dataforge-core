import com.jfrog.bintray.gradle.tasks.BintrayUploadTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.internal.artifact.FileBasedMavenArtifact
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.getting
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jfrog.gradle.plugin.artifactory.task.ArtifactoryTask

open class ScientifikMPPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply("org.jetbrains.kotlin.multiplatform")

        project.configure<KotlinMultiplatformExtension> {
            jvm {
                compilations.all {
                    kotlinOptions {
                        jvmTarget = "1.8"
                    }
                }
            }

            js {
                compilations.all {
                    kotlinOptions {
                        sourceMap = true
                        sourceMapEmbedSources = "always"
                        moduleKind = "commonjs"
                    }
                }
            }

            sourceSets.invoke {
                val commonMain by getting {
                    dependencies {
                        api(kotlin("stdlib"))
                    }
                }
                val commonTest by getting {
                    dependencies {
                        implementation(kotlin("test-common"))
                        implementation(kotlin("test-annotations-common"))
                    }
                }
                val jvmMain by getting {
                    dependencies {
                        api(kotlin("stdlib-jdk8"))
                    }
                }
                val jvmTest by getting {
                    dependencies {
                        implementation(kotlin("test"))
                        implementation(kotlin("test-junit"))
                    }
                }
                val jsMain by getting {
                    dependencies {
                        api(kotlin("stdlib-js"))
                    }
                }
                val jsTest by getting {
                    dependencies {
                        implementation(kotlin("test-js"))
                    }
                }
            }

            targets.all {
                sourceSets.all {
                    languageSettings.progressiveMode = true
                    languageSettings.enableLanguageFeature("InlineClasses")
                }
            }
        }


        project.tasks.filter { it is ArtifactoryTask || it is BintrayUploadTask }.forEach {
            it.doFirst {
                project.configure<PublishingExtension> {
                    publications
                        .filterIsInstance<MavenPublication>()
                        .forEach { publication ->
                            val moduleFile = project.buildDir.resolve("publications/${publication.name}/module.json")
                            if (moduleFile.exists()) {
                                publication.artifact(object : FileBasedMavenArtifact(moduleFile) {
                                    override fun getDefaultExtension() = "module"
                                })
                            }
                        }
                }
            }
        }

    }
}