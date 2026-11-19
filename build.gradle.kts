plugins {
    kotlin("jvm") version "2.1.21" apply false
    kotlin("plugin.serialization") version "2.1.21" apply false
}

allprojects {
    group = "com.hmhamster"
    version = "0.1.0"
}

/**
 * Development build: regenerates the front model types, builds the Vue front and the game plugin jar,
 * which the launcher's DEV watcher picks up automatically.
 */
val devFront = tasks.register<Exec>("devFront") {
    group = "development"
    description = "Builds the Vue front into front/dist (needs npm)"
    dependsOn(":back:generateTs")
    workingDir = projectDir
    val npm = if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) {
        listOf("cmd", "/c", "npm")
    } else {
        listOf("npm")
    }
    commandLine(npm + listOf("run", "build-front"))
}

tasks.register("devBuild") {
    group = "development"
    description = "Regenerates the model types, builds the front and the game plugin jar"
    dependsOn(":back:generateTs", devFront, ":back:jar")
}

/** Release build: front + dual-format game jar (with classes.dex), zipped for a GitHub release. */
tasks.register("releaseBuild") {
    group = "release"
    description = "Builds back/build/dist/cah.zip (front, jar and classes.dex)"
    dependsOn(":back:releaseZip")
}
