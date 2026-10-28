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
