import org.gradle.internal.os.OperatingSystem

plugins {
    `level-db-builds`
}

group = "com.github.lamba92"
version = "1.0-SNAPSHOT"

tasks {
    register<Zip>("mergeZips") {
        val os = OperatingSystem.current()
        val zipTasks = when {
            System.getenv("CI") == "true" -> listOf(linuxZip, androidZip, windowsZip, appleZip)
            os.isMacOsX -> listOf(appleZip, androidZip)
            os.isLinux -> listOf(linuxZip, androidZip)
            os.isWindows -> listOf(windowsZip, androidZip)
            else -> listOf(androidZip)
        }

        dependsOn(zipTasks)
        archiveBaseName = "leveldb"
        destinationDirectory = layout.buildDirectory.dir("archives")
        zipTasks.forEach { from(it.flatMap { it.archiveFile }.map { zipTree(it) }) }
    }
}
