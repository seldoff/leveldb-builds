import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import org.gradle.internal.os.OperatingSystem

plugins {
    `level-db-builds`
}

group = "com.github.lamba92"
version = "1.0-SNAPSHOT"

tasks {

    register<Zip>("testMergeZips") {
        val os = OperatingSystem.current()
        val zipTasks = when {
            System.getenv("CI") == "true" -> listOf(linuxZip, androidZip, windowsZip, appleZip)
            os.isMacOsX -> listOf(appleZip, androidZip)
            os.isLinux -> listOf(linuxZip, androidZip)
            os.isWindows -> listOf(windowsZip, androidZip)
            else -> listOf(androidZip)
        }

        dependsOn(zipTasks)
        archiveBaseName = "leveldb-test"
        destinationDirectory = layout.buildDirectory.dir("archives")
        zipTasks.forEach { from(it.flatMap { it.archiveFile }.map { zipTree(it) }) }
    }

    register<Zip>("mergeZips") {
        archiveBaseName = "leveldb"
        destinationDirectory = layout.buildDirectory.dir("archives")

        // Merge all zips in the project directory when running in CI
        layout.projectDirectory
            .asFile
            .toPath()
            .listDirectoryEntries()
            .filter { it.isDirectory() }
            .flatMap { it.listDirectoryEntries() }
            .filter { it.extension == "zip" }
            .forEach { from(zipTree(it)) }
    }
}
