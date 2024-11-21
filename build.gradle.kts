plugins {
    `level-db-builds`
}

group = "com.github.lamba92"
version = "1.0-SNAPSHOT"

tasks {
    register<Zip>("mergeZips") {
        val zipTasks = listOf(windowsZip, linuxZip, androidZip, appleZip)
        dependsOn(zipTasks)
        archiveBaseName = "leveldb"
        destinationDirectory = layout.buildDirectory.dir("archives")
        zipTasks.forEach { from(it.flatMap { it.archiveFile }.map { zipTree(it) }) }
    }
}
