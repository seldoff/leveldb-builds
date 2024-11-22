import org.gradle.internal.extensions.stdlib.capitalized
import org.gradle.internal.os.OperatingSystem
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.registering
import kotlin.io.path.absolutePathString
import kotlin.io.path.relativeTo

val leveldbBuildDir = layout.buildDirectory.dir("compilations/leveldb")

val leveldbBuildDirPath = leveldbBuildDir.map { it.asFile.toPath() }

data class BuildConfig(
    val release: Boolean,
    val shared: Boolean,
)

val matrix = buildList {
    add(BuildConfig(release = false, shared = false))
    add(BuildConfig(release = false, shared = true))
    add(BuildConfig(release = true, shared = false))
    add(BuildConfig(release = true, shared = true))
}

fun withMatrix(
    platformName: String,
    action: MutableList<TaskProvider<BuildLeveldb>>.(
        release: Boolean,
        isShared: Boolean,
        baseTaskName: String,
        dirPath: (String) -> String
    ) -> Unit
) = buildList {
    matrix.forEach { (isRelease, isShared) ->
        val taskName = buildString {
            append("buildLeveldb")
            when {
                isShared -> append("Shared")
                else -> append("Static")
            }
            when {
                isRelease -> append("Release")
                else -> append("Debug")
            }
            append(platformName.capitalized())
        }
        val dirPath = { arch: String ->
            buildString {
                append("${platformName.decapitalized()}/")
                when {
                    isShared -> append("shared/")
                    else -> append("static/")
                }
                append("$arch/")
                when {
                    isRelease -> append("release/")
                    else -> append("debug/")
                }
            }
        }
        action(isRelease, isShared, taskName, dirPath)
    }
}

// Windows
val levelDbSourcesDir = layout.projectDirectory.dir("leveldb")

val winTasks =
    withMatrix("windows") { isRelease, isShared, baseTaskName, dirPath ->

        add(tasks.register<BuildLeveldb>("${baseTaskName}Arm64") {
            onlyIf { OperatingSystem.current().isWindows }
            windowsCmakeName = "MinGW Makefiles"
            debug = !isRelease
            shared = isShared
            cCompiler = "clang"
            cxxCompiler = "clang++"
            systemName = "Windows"
            systemProcessorName = "ARM64"
            val ext = when {
                isShared -> "dll"
                else -> "lib"
            }
            cxxFlags = listOf("-D_CRT_SECURE_NO_WARNINGS", "-Dstrdup=_strdup", "--target=aarch64-windows")
            outputDir(leveldbBuildDir.map { it.dir(dirPath("arm64")) }, "leveldb.$ext")
            sourcesDir = levelDbSourcesDir
        })

        add(tasks.register<BuildLeveldb>("${baseTaskName}X64") {
            onlyIf { OperatingSystem.current().isWindows }
            windowsCmakeName = "MinGW Makefiles"
            debug = !isRelease
            shared = isShared
            cCompiler = "C:\\ProgramData\\Chocolatey\\bin\\gcc.exe"
            cxxCompiler = "C:\\ProgramData\\Chocolatey\\bin\\g++.exe"
            val basicFlags = listOf("-static-libgcc", "-static-libstdc++")
            cxxFlags = when {
                isShared -> basicFlags + "-lpthread"
                else -> basicFlags
            }
            val ext = when {
                isShared -> "dll"
                else -> "a"
            }
            outputDir(leveldbBuildDir.map { it.dir(dirPath("x64")) }, "libleveldb.$ext")
            sourcesDir = levelDbSourcesDir
        })
    }

val buildLeveldbWindows by tasks.registering {
    group = "build"
    dependsOn(winTasks)
    onlyIf { OperatingSystem.current().isWindows }
    winTasks.forEach { task ->
        outputs.file(task.flatMap { it.outputArtifact })
    }
}

tasks.register<Zip>("windowsZip") {
    from(buildLeveldbWindows) {
        eachFile {
            path = file.toPath().relativeTo(leveldbBuildDirPath.get()).toString()
        }
    }
    archiveBaseName = "leveldb-windows"
    destinationDirectory = layout.buildDirectory.dir("archives")
}

// Linux
val linuxTasks =
    withMatrix("linux") { isRelease, isShared, baseTaskName, dirPath ->
        val flags = when {
            isShared -> listOf("-static-libgcc", "-static-libstdc++")
            else -> emptyList()
        }
        val ext = when {
            isShared -> "so"
            else -> "a"
        }
        add(tasks.register<BuildLeveldb>("${baseTaskName}X64") {
            onlyIf { OperatingSystem.current().isLinux }
            debug = !isRelease
            shared = isShared
            cCompiler = "gcc-9"
            cxxCompiler = "g++-9"
            cxxFlags = flags
            outputDir(leveldbBuildDir.map { it.dir(dirPath("x64")) }, "libleveldb.$ext")
            sourcesDir = levelDbSourcesDir
        })

        add(tasks.register<BuildLeveldb>("${baseTaskName}Arm64") {
            onlyIf { OperatingSystem.current().isLinux }
            debug = !isRelease
            shared = isShared
            cCompiler = "aarch64-linux-gnu-gcc-9"
            cxxCompiler = "aarch64-linux-gnu-g++-9"
            cxxFlags = flags
            outputDir(leveldbBuildDir.map { it.dir(dirPath("arm64")) }, "libleveldb.$ext")
            sourcesDir = levelDbSourcesDir
        })

        add(tasks.register<BuildLeveldb>("${baseTaskName}Armv7a") {
            onlyIf { OperatingSystem.current().isLinux }
            debug = !isRelease
            shared = isShared
            cCompiler = "arm-linux-gnueabihf-gcc-9"
            cxxCompiler = "arm-linux-gnueabihf-g++-9"
            cxxFlags = flags + "-Wno-psabi"
            outputDir(leveldbBuildDir.map { it.dir(dirPath("armv7a")) }, "libleveldb.$ext")
            sourcesDir = levelDbSourcesDir
        })
    }

val buildLeveldbLinux by tasks.registering {
    group = "build"
    dependsOn(linuxTasks)
    onlyIf { OperatingSystem.current().isLinux }
    linuxTasks.forEach { task ->
        outputs.file(task.flatMap { it.outputArtifact })
    }
}

tasks.register<Zip>("linuxZip") {
    dependsOn(linuxTasks)
    from(buildLeveldbLinux) {
        eachFile {
            path = file.toPath().relativeTo(leveldbBuildDirPath.get()).toString()
        }
    }
    archiveBaseName = "leveldb-linux"
    destinationDirectory = layout.buildDirectory.dir("archives")
}

// Android
val androidTasks =
    withMatrix("android") { isRelease, isShared, baseTaskName, dirPath ->
        val stlType = when {
            isShared -> "c++_shared"
            else -> "c++_static"
        }
        val ext = when {
            isShared -> "so"
            else -> "a"
        }
        add(tasks.register<BuildLeveldb>("${baseTaskName}Arm64") {
            systemName = "Android"
            androidNdkPath = findAndroidNdk()?.absolutePathString()
            systemVersion = 35
            androidStlType = stlType
            androidAbi = "arm64-v8a"
            shared = isShared
            debug = !isRelease
            outputDir(leveldbBuildDir.map { it.dir(dirPath("arm64")) }, "libleveldb.$ext")
            sourcesDir = levelDbSourcesDir
        })

        add(tasks.register<BuildLeveldb>("${baseTaskName}ArmV7a") {
            systemName = "Android"
            androidNdkPath = findAndroidNdk()?.absolutePathString()
            systemVersion = 35
            androidStlType = stlType
            androidAbi = "armeabi-v7a"
            shared = isShared
            debug = !isRelease
            outputDir(leveldbBuildDir.map { it.dir(dirPath("armv7a")) }, "libleveldb.$ext")
            sourcesDir = levelDbSourcesDir
        })

        add(tasks.register<BuildLeveldb>("${baseTaskName}X86") {
            systemName = "Android"
            androidNdkPath = findAndroidNdk()?.absolutePathString()
            systemVersion = 35
            androidStlType = stlType
            androidAbi = "x86"
            shared = isShared
            debug = !isRelease
            outputDir(leveldbBuildDir.map { it.dir(dirPath("x86")) }, "libleveldb.$ext")
            sourcesDir = levelDbSourcesDir
        })

        add(tasks.register<BuildLeveldb>("${baseTaskName}X64") {
            systemName = "Android"
            androidNdkPath = findAndroidNdk()?.absolutePathString()
            systemVersion = 35
            androidStlType = stlType
            androidAbi = "x86_64"
            shared = isShared
            debug = !isRelease
            outputDir(leveldbBuildDir.map { it.dir(dirPath("x64")) }, "libleveldb.$ext")
            sourcesDir = levelDbSourcesDir
        })
    }

val buildLeveldbAndroid by tasks.registering {
    group = "build"
    dependsOn(androidTasks)
    androidTasks.forEach { task ->
        outputs.file(task.flatMap { it.outputArtifact })
    }
}

tasks.register<Zip>("androidZip") {
    dependsOn(buildLeveldbAndroid)
    from(buildLeveldbAndroid) {
        eachFile {
            path = file.toPath().relativeTo(leveldbBuildDirPath.get()).toString()
        }
    }
    archiveBaseName = "leveldb-android"
    destinationDirectory = layout.buildDirectory.dir("archives")
}

// Apple
data class AppleTarget(
    val arch: String,
    val sysName: String,
    val sysRoot: String,
)

val appleTargets = listOf(
    // mac
    AppleTarget("x86_64", "Darwin", "macosx"),
    AppleTarget("arm64", "Darwin", "macosx"),

    // ios
    AppleTarget("arm64", "iOS", "iphoneos"),
    AppleTarget("arm64", "iOS", "iphonesimulator"),
    AppleTarget("x86_64", "iOS", "iphonesimulator"),

    // tvos
    AppleTarget("arm64", "tvOS", "appletvos"),
    AppleTarget("arm64", "tvOS", "appletvsimulator"),
    AppleTarget("x86_64", "tvOS", "appletvsimulator"),

    // watchos
    AppleTarget("arm64", "watchOS", "watchos"),
    AppleTarget("arm64", "watchOS", "watchsimulator"),
    AppleTarget("x86_64", "watchOS", "watchsimulator"),
)

val appleTasks = appleTargets.flatMap { (arch, sysName, sysRoot) ->
    withMatrix(sysRoot) { isRelease, isShared, baseTaskName, dirPath ->

        if (isShared && sysRoot != "macosx") return@withMatrix
        val extension = when {
            isShared -> "dylib"
            else -> "a"
        }
        add(tasks.register<BuildLeveldb>("${baseTaskName}${arch.capitalized()}") {
            onlyIf { OperatingSystem.current().isMacOsX }
            shared = isShared
            debug = !isRelease
            systemName = sysName
            osxArch = arch
            osxSysroot = sysRoot
            outputDir(leveldbBuildDir.map { it.dir(dirPath(arch)) }, "libleveldb.$extension")
            sourcesDir = levelDbSourcesDir
        })
    }
}

val buildLeveldbApple by tasks.registering {
    group = "build"
    dependsOn(appleTasks)
    onlyIf { OperatingSystem.current().isMacOsX }
    appleTasks.forEach { task ->
        outputs.file(task.flatMap { it.outputArtifact })
    }
}

tasks.register<Zip>("appleZip") {
    from(buildLeveldbApple) {
        eachFile {
            path = file.toPath().relativeTo(leveldbBuildDirPath.get()).toString()
        }
    }
    archiveBaseName = "leveldb-apple"
    destinationDirectory = layout.buildDirectory.dir("archives")
}
