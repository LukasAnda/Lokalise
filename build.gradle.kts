import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithHostTests

plugins {
    application
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.github.johnrengelman.shadow")
    id("dev.petuska.npm.publish")
    id("com.louiscad.complete-kotlin") version "1.1.0"
}

group = "cli"
version = "0.0.1"

val program = "transkribe"

repositories {
    google()
    mavenCentral()
}

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.6.10")
    testImplementation(Testing.junit.jupiter.params)
    testRuntimeOnly(Testing.junit.jupiter.engine)
}

application {
    mainClass.set("cli.JvmMainKt")
}

val hostOs = System.getProperty("os.name")
val nativeTarget = when {
    hostOs == "Mac OS X" -> "MacosX64"
    hostOs == "Linux" -> "LinuxX64"
    hostOs.startsWith("Windows") -> "MingwX64"
    else -> throw GradleException("Host $hostOs is not supported in Kotlin/Native.")
}

fun KotlinNativeTargetWithHostTests.configureTarget() =
    binaries { executable { entryPoint = "main" } }

kotlin {

    macosX64 { configureTarget() }
    mingwX64 { configureTarget() }
    linuxX64 { configureTarget() }

    val jvmTarget = jvm()

    val node = js(IR) {
        nodejs()
        binaries.executable()
        useCommonJs()
    }

    sourceSets {
        all {
            languageSettings.useExperimentalAnnotation("kotlin.RequiresOptIn")
        }

        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.4")
//                implementation("com.github.ajalt.clikt:clikt:3.4.0")
//                implementation("com.github.ajalt.mordant:mordant:2.0.0-beta4")
                implementation("com.squareup.okio:okio-multiplatform:3.0.0-alpha.9")
                implementation(KotlinX.coroutines.core)

                /// implementation(Ktor.client.core)
                /// implementation(Ktor.client.serialization)
                implementation(KotlinX.serialization.core)
                implementation(KotlinX.serialization.json)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(Kotlin.test.common)
                implementation(Kotlin.test.annotationsCommon)
            }
        }
        getByName("jvmMain") {
            dependsOn(commonMain)
            dependencies {
                implementation(Ktor.client.okHttp)
                /// implementation(Square.okHttp3.okHttp)
            }
        }
        getByName("jvmTest") {
            dependencies {
                implementation(Testing.junit.jupiter.api)
                implementation(Testing.junit.jupiter.engine)
                implementation(Kotlin.test.junit5)
            }
        }
        val nativeMain by creating {
            dependsOn(commonMain)
            dependencies {
                /// implementation(Ktor.client.curl)
            }
        }
        val nativeTest by creating {
            dependsOn(commonTest)
        }
        val posixMain by creating {
            dependsOn(nativeMain)
        }
        val posixTest by creating {
            dependsOn(nativeTest)
        }
        arrayOf("macosX64", "linuxX64").forEach { targetName ->
            getByName("${targetName}Main").dependsOn(posixMain)
            getByName("${targetName}Test").dependsOn(posixTest)
        }
        arrayOf("macosX64", "linuxX64", "mingwX64").forEach { targetName ->
            getByName("${targetName}Main").dependsOn(nativeMain)
            getByName("${targetName}Test").dependsOn(nativeTest)
        }
        getByName("jsMain") {
            dependencies {
                implementation("com.squareup.okio:okio-nodefilesystem-js:_")
//                implementation(KotlinX.nodeJs)
            }
        }
        getByName("jsTest") {
            dependsOn(nativeTest)
            dependencies {
                implementation(Kotlin.test.jsRunner)
                implementation(kotlin("test-js"))
            }
        }

        sourceSets {
            all {
                languageSettings.useExperimentalAnnotation("kotlin.RequiresOptIn")
                languageSettings.useExperimentalAnnotation("okio.ExperimentalFileSystem")
            }
        }
    }

    tasks.named<KotlinJsCompile>("compileKotlinJs").configure {
        kotlinOptions.moduleKind = "commonjs"
    }

    tasks.withType<JavaExec> {
        // code to make run task in kotlin multiplatform work
        val compilation = jvmTarget.compilations.getByName<KotlinJvmCompilation>("main")

        val classes = files(
            compilation.runtimeDependencyFiles,
            compilation.output.allOutputs
        )
        classpath(classes)
    }
    tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
        archiveBaseName.set(project.name)
        archiveClassifier.set("")
        archiveVersion.set("")

        from(jvmTarget.compilations.getByName("main").output)
        configurations = mutableListOf(
            jvmTarget.compilations.getByName("main").compileDependencyFiles,
            jvmTarget.compilations.getByName("main").runtimeDependencyFiles
        )
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.register<Copy>("install") {
    group = "run"
    description = "Build the native executable and install it"
    val destDir = "/usr/local/bin"


    dependsOn("runDebugExecutable$nativeTarget")
    val targetLowercase = nativeTarget.first().toLowerCase() + nativeTarget.substring(1)
    val folder = "build/bin/$targetLowercase/debugExecutable"
    from(folder) {
        include("${rootProject.name}.kexe")
        rename { program }
    }
    into(destDir)
    doLast {
        println("$ cp $folder/${rootProject.name}.kexe $destDir/$program")
    }
}

tasks.register("allRun") {
    group = "run"
    description = "Run $program on the JVM, on Node and natively"
    dependsOn("run", "jsNodeRun", "runDebugExecutable$nativeTarget")
}

tasks.register("runOnGitHub") {
    group = "run"
    description = "CI with Github Actions : .github/workflows/runOnGitHub.yml"
    dependsOn("allTests", "allRun")
}

// See https://github.com/mpetuska/npm-publish
npmPublishing {
    dry = false
    repositories {
//        val token = System.getenv("NPM_AUTH_TOKEN")
        val token = "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"
        if (token == null) {
            println("No environment variable NPM_AUTH_TOKEN found, using dry-run for publish")
            dry = true
        } else {
            repository("npmjs") {
                registry = uri("https://registry.npmjs.org")
                authToken = token
            }
        }
    }
    publications {
        publication("js") {
            readme = file("README.md")
            packageJson {
                bin = mutableMapOf(
                    Pair(program, "./$program")
                )
                main = program
                private = false
                keywords = jsonArray(
                    "kotlin", "git", "bash"
                )
            }
            files { assemblyDir -> // Specifies what files should be packaged. Preconfigured for default publications, yet can be extended if needed
                from("$assemblyDir/../dir")
                from("bin") {
                    include(program)
                }
            }
        }

    }
}

interface Injected {
    @get:Inject
    val exec: ExecOperations
    @get:Inject
    val fs: FileSystemOperations
}

tasks.register("completions") {
    group = "run"
    description = "Generate Bash/Zsh/Fish completion files"
    dependsOn(":install")
    val injected = project.objects.newInstance<Injected>()
    val shells = listOf(
        Triple("bash", file("completions/transkribe.bash"), "/usr/local/etc/bash_completion.d"),
        Triple("zsh", file("completions/_transkribe.zsh"), "/usr/local/share/zsh/site-functions"),
        Triple("fish", file("completions/transkribe.fish"), "/usr/local/share/fish/vendor_completions.d"),
    )
    for ((SHELL, FILE, INSTALL) in shells) {
        actions.add {
            println("Updating   $SHELL completion file at $FILE")
            injected.exec.exec {
                commandLine("transkribe", "--generate-completion", SHELL)
                standardOutput = FILE.outputStream()
            }
            println("Installing $SHELL completion into $INSTALL")
            injected.fs.copy {
                from(FILE)
                into(INSTALL)
            }
        }
    }
    doLast {
        println("On macOS, follow those instructions to configure shell completions")
        println("👀 https://docs.brew.sh/Shell-Completion 👀")
    }
}