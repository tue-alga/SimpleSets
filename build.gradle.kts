import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.internal.os.OperatingSystem
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "com.stevenvdb"
version = "1.0"

val applicationMainClass = "MainKt"

/**  ## additional ORX features to be added to this project */
val orxFeatures = setOf(
    "orx-color",
    "orx-compositor",
    "orx-shapes",
    "orx-triangulation",
    "orx-parameters",
    "orx-marching-squares",
)

// ------------------------------------------------------------------------------------------------------------------ //

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    kotlin("multiplatform") version libs.versions.kotlin.get()
    kotlin("plugin.serialization") version "2.0.0"
    java
    alias(libs.plugins.shadow)
    alias(libs.plugins.runtime)
    alias(libs.plugins.gitarchive.tomarkdown).apply(false)
    id("io.github.turansky.kfc.webpack") version "8.1.0"
}

tasks {
    wrapper {
        gradleVersion = "8.8"
    }
}

tasks {
    patchWebpackConfig {
        patch(
            "don't clean",
            """
                if (!!config.output) {
                  config.output.clean = false
                }
                """.trimIndent()
        )

        patch(
            "raw load ipe files",
            """
            config.module.rules.push(
              {
                test: /\.ipe${'$'}/i,
                loader: 'raw-loader',
                options: {
                  esModule: false,
                },
            }
            )
            """.trimIndent()
        )
    }
}

fun String.runCommand(workingDir: File) {
    ProcessBuilder(*split(" ").toTypedArray())
        .directory(workingDir)
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .start()
        .waitFor(60, TimeUnit.MINUTES)
}

tasks.register("pyHttpServer") {
    doLast {
        "py -m http.server".runCommand(File("build/distributions/"))
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url = uri("https://repo.eclipse.org/content/groups/releases")
    }
    maven {
        url = uri("https://repo.eclipse.org/content/repositories/jts-snapshots")
    }
}

val openrndrVersion = libs.versions.openrndr.get()
val orxVersion = libs.versions.orx.get()
val os = if (project.hasProperty("targetPlatform")) {
    val supportedPlatforms = setOf("windows", "macos", "linux-x64", "linux-arm64")
    val platform: String = project.property("targetPlatform") as String
    if (platform !in supportedPlatforms) {
        throw IllegalArgumentException("target platform not supported: $platform")
    } else {
        platform
    }
} else {
    val cos = OperatingSystem.current()
    if (cos.isWindows) {
        "windows"
    } else if (cos.isMacOsX) {
        when (val h = DefaultNativePlatform("current").architecture.name) {
            "aarch64", "arm-v8" -> "macos-arm64"
            else -> "macos"
        }
    } else if (cos.isLinux) {
        when (val h = DefaultNativePlatform("current").architecture.name) {
            "x86-64" -> "linux-x64"
            "aarch64" -> "linux-arm64"
            else -> throw IllegalArgumentException("architecture not supported: $h")
        }
    }
    else throw IllegalArgumentException("os not supported")
}
fun openrndr(module: String) = "org.openrndr:openrndr-$module:$openrndrVersion"
fun orx(module: String) = "org.openrndr.extra:$module:$orxVersion"
fun openrndrNatives(module: String) = "org.openrndr:openrndr-$module-natives-$os:$openrndrVersion"
fun kotlinw(target: String): String = "org.jetbrains.kotlin-wrappers:kotlin-$target"

val dummyAttribute = Attribute.of("dummy", String::class.java)

kotlin {
    js("js", IR) {
        binaries.executable()
        browser {
            commonWebpackConfig {
                outputFileName = "SimpleSets.js"
            }
        }
    }

    js("webworker", IR) {
        attributes.attribute(dummyAttribute, "KT-55751")
        binaries.executable()
        browser{
            commonWebpackConfig {
                outputFileName = "worker.js"
            }
        }
    }

    jvm {

    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(openrndr("application"))
                for (feature in orxFeatures) {
                    implementation(orx(feature))
                }

                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(orx("orx-gui"))
                implementation(orx("orx-olive"))
                runtimeOnly(openrndr("gl3"))
                runtimeOnly(openrndrNatives("gl3"))
                implementation(openrndr("openal"))
                runtimeOnly(openrndrNatives("openal"))
                implementation(openrndr("svg"))
                implementation(openrndr("animatable"))
                implementation(openrndr("extensions"))
                implementation(openrndr("filter"))
                implementation("org.locationtech.jts:jts-core:1.19.0")
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.junit.jupiter:junit-jupiter:5.8.1")
            }
        }

        val jsCommon by creating {
            dependsOn(commonMain)
        }

        val jsMain by getting {
            dependsOn(jsCommon)

            dependencies {
                // React, React DOM + Wrappers
                implementation(project.dependencies.enforcedPlatform(kotlinw("wrappers-bom:1.0.0-pre.757")))
                implementation(kotlinw("react"))
                implementation(kotlinw("react-dom"))

                // Kotlin React Emotion (CSS)
                implementation(kotlinw("emotion"))

                // OPENRNDR
                implementation(openrndr("draw"))

                // Webpack loaders
                implementation(devNpm("raw-loader", "4.0.2"))
            }
        }

        val webworkerMain by getting {
            dependsOn(jsCommon)

            dependencies {

            }
        }
    }
}

// ------------------------------------------------------------------------------------------------------------------ //

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

// ------------------------------------------------------------------------------------------------------------------ //

project.setProperty("mainClassName", applicationMainClass)
tasks {
    named<ShadowJar>("shadowJar") {
        manifest {
            attributes["Main-Class"] = applicationMainClass
        }
        minimize {
            exclude(dependency("org.openrndr:openrndr-gl3:.*"))
            exclude(dependency("org.jetbrains.kotlin:kotlin-reflect:.*"))
        }
    }
    named<org.beryx.runtime.JPackageTask>("jpackage") {
        doLast {
            when (OperatingSystem.current()) {
                OperatingSystem.WINDOWS, OperatingSystem.LINUX -> {
                    copy {
                        from("data") {
                            include("**/*")
                        }
                        into("build/jpackage/openrndr-application/data")
                    }
                }
                OperatingSystem.MAC_OS -> {
                    copy {
                        from("data") {
                            include("**/*")
                        }
                        into("build/jpackage/openrndr-application.app/data")
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------------------------ //

tasks.register<Zip>("jpackageZip") {
    archiveFileName.set("openrndr-application.zip")
    from("$buildDir/jpackage") {
        include("**/*")
    }
}
tasks.findByName("jpackageZip")?.dependsOn("jpackage")

// ------------------------------------------------------------------------------------------------------------------ //

runtime {
    jpackage {
        imageName = "openrndr-application"
        skipInstaller = true
        if (OperatingSystem.current() == OperatingSystem.MAC_OS) jvmArgs.add("-XstartOnFirstThread")
    }
    options.set(listOf("--strip-debug", "--compress", "1", "--no-header-files", "--no-man-pages"))
    modules.set(listOf("jdk.unsupported", "java.management"))
}

// ------------------------------------------------------------------------------------------------------------------ //

tasks.register<org.openrndr.extra.gitarchiver.GitArchiveToMarkdown>("gitArchiveToMarkDown") {
    historySize.set(20)
}

// ------------------------------------------------------------------------------------------------------------------ //

if (properties["openrndr.tasks"] == "true") {
    task("create executable jar for $applicationMainClass") {
        group = " \uD83E\uDD8C OPENRNDR"
        dependsOn("jar")
    }

    task("run $applicationMainClass") {
        group = " \uD83E\uDD8C OPENRNDR"
        dependsOn("run")
    }

    task("create standalone executable for $applicationMainClass") {
        group = " \uD83E\uDD8C OPENRNDR"
        dependsOn("jpackageZip")
    }

    task("add IDE file scopes") {
        group = " \uD83E\uDD8C OPENRNDR"
        val scopesFolder = File("${project.projectDir}/.idea/scopes")
        scopesFolder.mkdirs()

        val files = listOf(
            "Code" to "file:*.kt||file:*.frag||file:*.vert||file:*.glsl",
            "Text" to "file:*.txt||file:*.md||file:*.xml||file:*.json",
            "Gradle" to "file[*buildSrc*]:*/||file:*gradle.*||file:*.gradle||file:*/gradle-wrapper.properties||file:*.toml",
            "Images" to "file:*.png||file:*.jpg||file:*.dds||file:*.exr"
        )
        files.forEach { (name, pattern) ->
            val file = File(scopesFolder, "__$name.xml")
            if (!file.exists()) {
                file.writeText(
                    """
                    <component name="DependencyValidationManager">
                      <scope name=" ★ $name" pattern="$pattern" />
                    </component>
                    """.trimIndent()
                )
            }
        }
    }
}
