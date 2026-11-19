import java.util.Properties

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    implementation("com.hmhamster:boardgame-api:0.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")

    testImplementation(kotlin("test"))
}

val versionProperties = layout.buildDirectory.file("generated/version/version.properties")
val generateVersionProperties = tasks.register("generateVersionProperties") {
    val version = project.version.toString()
    outputs.file(versionProperties)
    doLast {
        versionProperties.get().asFile.apply {
            parentFile.mkdirs()
            writeText("version=$version\n")
        }
    }
}

tasks.processResources {
    dependsOn(generateVersionProperties)
    from(versionProperties)
}

/** Release artefact: back/module.jar, back/classes.dex (when built on a machine with d8). */
val moduleJar = tasks.named<Jar>("jar")

/** Generates the front's model types from the Kotlin serializers (validation-only for now). */
val generateTs = tasks.register<JavaExec>("generateTs") {
    group = "codegen"
    description = "Generates front/src/domain/generated/types.ts from the Kotlin models"
    dependsOn(tasks.named("classes"))
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("boardgame.cah.TsExport")
    args(layout.projectDirectory.file("../front/src/domain/generated/types.ts").asFile.absolutePath)
}

val isWindows = System.getProperty("os.name").lowercase().contains("win")

fun androidSdkDir(): File {
    val local = rootProject.file("local.properties")
    val configured = if (local.isFile) {
        Properties().apply { local.inputStream().use { load(it) } }.getProperty("sdk.dir")
    } else {
        System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
    }
    return File(configured ?: error("Android SDK not found: add sdk.dir to local.properties or set ANDROID_HOME"))
}

val dex = tasks.register("dex") {
    group = "release"
    description = "Converts the game jar to classes.dex with d8"
    dependsOn(moduleJar)
    val jarFile = moduleJar.flatMap { it.archiveFile }
    val dexDir = layout.buildDirectory.dir("dex")
    inputs.file(jarFile)
    outputs.dir(dexDir)

    doLast {
        val sdk = androidSdkDir()
        val d8 = File(sdk, "build-tools/35.0.0/" + if (isWindows) "d8.bat" else "d8")
        val output = dexDir.get().asFile.apply { deleteRecursively(); mkdirs() }
        val command = mutableListOf<String>()
        if (isWindows) command += listOf("cmd", "/c", d8.absolutePath) else command += d8.absolutePath
        command += listOf(
            "--release", "--min-api", "26",
            "--lib", File(sdk, "platforms/android-35/android.jar").absolutePath,
            "--output", output.absolutePath,
            jarFile.get().asFile.absolutePath
        )
        val process = ProcessBuilder(command).redirectErrorStream(true).start()
        val log = process.inputStream.bufferedReader().readText()
        check(process.waitFor() == 0) { "d8 failed:\n$log" }
        check(File(output, "classes.dex").isFile) { "d8 produced no classes.dex:\n$log" }
    }
}

val dualJar = tasks.register<Jar>("dualJar") {
    group = "release"
    description = "Game jar usable by URLClassLoader and DexClassLoader"
    dependsOn(dex)
    archiveFileName.set("cah.jar")
    destinationDirectory.set(layout.buildDirectory.dir("release/back"))
    from(sourceSets.main.get().output)
    from(layout.buildDirectory.dir("dex"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

/**
 * Game release asset: the back jar plus the built front assets. The asset is intentionally named
 * `cah.zip` so the core installs it into `games-modules/cah/` (stable directory for the front URL);
 * the version lives in the release tag and in `version.properties`.
 */
val releaseZip = tasks.register<Zip>("releaseZip") {
    group = "release"
    description = "Packages the CAH module for a GitHub release"
    dependsOn(dualJar)
    archiveFileName.set("cah.zip")
    destinationDirectory.set(layout.buildDirectory.dir("dist"))
    from(layout.buildDirectory.dir("release/back")) { into("back") }
    from(file("../front/dist")) { into("front") }
    doFirst {
        require(file("../front/dist").isDirectory) {
            "Front build missing at ${file("../front/dist")}. Run `npm run build-front` in the cah repository first."
        }
    }
}
