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
    val isDebug: Boolean,
    val isShared: Boolean,
)

val matrix = buildList {
    add(BuildConfig(isDebug = false, isShared = false))
    add(BuildConfig(isDebug = false, isShared = true))
    add(BuildConfig(isDebug = true, isShared = false))
    add(BuildConfig(isDebug = true, isShared = true))
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
    matrix.forEach { (isDebug, isShared) ->
        val taskName = buildString {
            append("buildLeveldb")
            when {
                isShared -> append("Shared")
                else -> append("Static")
            }
            when {
                isDebug -> append("Debug")
                else -> append("Release")
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
                    isDebug -> append("debug/")
                    else -> append("release/")
                }
            }
        }
        action(isDebug, isShared, taskName, dirPath)
    }
}

// Windows
val levelDbSourcesDir = layout.projectDirectory.dir("leveldb")

val winTasks =
    withMatrix("windows") { isDebug, isShared, baseTaskName, dirPath ->
        val flags = listOf("-static-libgcc", "-static-libstdc++", "-lpthread")
        val ext = when {
            isShared -> "dll"
            else -> "a"
        }
        add(tasks.register<BuildLeveldb>("${baseTaskName}Arm64") {
            onlyIf { false } // cross-compilation is not supported
            debug = isDebug
            shared = isShared
            cCompiler = "aarch64-w64-mingw32-gcc"
            cxxCompiler = "aarch64-w64-mingw32-g++"
            systemName = "Windows"
            systemProcessorName = "ARM64"
            cxxFlags = flags
            outputDir(leveldbBuildDir.map { it.dir(dirPath("arm64")) }, "libleveldb.$ext")
            sourcesDir = levelDbSourcesDir
        })

        add(tasks.register<BuildLeveldb>("${baseTaskName}X64") {
            debug = isDebug
            shared = isShared
            cCompiler = "x86_64-w64-mingw32-gcc"
            cxxCompiler = "x86_64-w64-mingw32-g++"
            systemName = "Windows"
            systemProcessorName = "x86_64"
            cxxStandard = "11"
            cxxFlags = flags
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
    withMatrix("linux") { isDebug, isShared, baseTaskName, dirPath ->
        val flags = when {
            isShared -> listOf("-static-libgcc", "-static-libstdc++")
            else -> listOf("-D_GLIBCXX_USE_CXX11_ABI=0")
        }
        val ext = when {
            isShared -> "so"
            else -> "a"
        }
        add(tasks.register<BuildLeveldb>("${baseTaskName}X64") {
            debug = isDebug
            shared = isShared
            cCompiler = "gcc-8"
            cxxCompiler = "g++-8"
            cxxFlags = flags
            outputDir(leveldbBuildDir.map { it.dir(dirPath("x64")) }, "libleveldb.$ext")
            sourcesDir = levelDbSourcesDir
        })

        add(tasks.register<BuildLeveldb>("${baseTaskName}Arm64") {
            debug = isDebug
            shared = isShared
            cCompiler = "aarch64-linux-gnu-gcc-8"
            cxxCompiler = "aarch64-linux-gnu-g++-8"
            cxxFlags = flags
            outputDir(leveldbBuildDir.map { it.dir(dirPath("arm64")) }, "libleveldb.$ext")
            sourcesDir = levelDbSourcesDir
        })

        add(tasks.register<BuildLeveldb>("${baseTaskName}Armv7a") {
            debug = isDebug
            shared = isShared
            cCompiler = "arm-linux-gnueabihf-gcc-8"
            cxxCompiler = "arm-linux-gnueabihf-g++-8"
            cxxFlags = flags + "-Wno-psabi"
            outputDir(leveldbBuildDir.map { it.dir(dirPath("armv7a")) }, "libleveldb.$ext")
            sourcesDir = levelDbSourcesDir
        })
    }

val buildLeveldbLinux by tasks.registering {
    group = "build"
    dependsOn(linuxTasks)
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
    withMatrix("android") { isDebug, isShared, baseTaskName, dirPath ->
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
            debug = isDebug
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
            debug = isDebug
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
            debug = isDebug
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
            debug = isDebug
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
    withMatrix(sysRoot) { isDebug, isShared, baseTaskName, dirPath ->

        if (isShared && sysRoot != "macosx") return@withMatrix
        val extension = when {
            isShared -> "dylib"
            else -> "a"
        }
        add(tasks.register<BuildLeveldb>("${baseTaskName}${arch.capitalized()}") {
            onlyIf { OperatingSystem.current().isMacOsX }
            shared = isShared
            debug = isDebug
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
