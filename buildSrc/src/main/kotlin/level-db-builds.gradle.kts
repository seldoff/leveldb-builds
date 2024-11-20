import org.gradle.internal.extensions.stdlib.capitalized
import org.gradle.internal.os.OperatingSystem
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.registering
import kotlin.io.path.absolutePathString

val leveldbBuildDir = layout.buildDirectory.dir("compilations/leveldb")

data class BuildConfig(
    val release: Boolean,
    val shared: Boolean,
)

val matrix = buildList {
    add(BuildConfig(false, false))
    add(BuildConfig(false, true))
    add(BuildConfig(true, false))
    add(BuildConfig(true, true))
}

fun withMatrix(
    paltformName: String,
    action: MutableList<TaskProvider<BuildLeveldb>>.(
        release: Boolean,
        isShared: Boolean,
        baseTaskName: String,
        baseDirPath: String
    ) -> Unit
) {
//    buildList {
//        matrix.forEach {  }
//    }
}

// Windows
val winTasks = buildList {
    matrix.forEach { (release, isShared) ->
        val taskName = buildString {
            append("buildLeveldb")
            when {
                isShared -> append("Shared")
                else -> append("Static")
            }
            when {
                release -> append("Release")
                else -> append("Debug")
            }
            append("Windows")
        }
        val flags = buildList {
            addAll("-static-libgcc", "-static-libstdc++")
            if (isShared) add("-lpthread")
        }
        val dirPath = buildString {
            append("windows/")
            when {
                isShared -> append("shared/")
                else -> append("static/")
            }
            when {
                release -> append("release/")
                else -> append("debug/")
            }
        }
        add(tasks.register<BuildLeveldb>("${taskName}Arm64") {
            onlyIf { OperatingSystem.current().isWindows }
            windowsCmakeName = "MinGW Makefiles"
            debug = !release
            shared = isShared
            cCompiler = "clang"
            cxxCompiler = "clang++"
            systemName = "Windows"
            systemProcessorName = "ARM64"
            cxxFlags = flags
            outputDir(leveldbBuildDir.map { it.dir("$dirPath/arm64") }, "libleveldb.dll")
            sourcesDir = layout.projectDirectory.dir("leveldb")
        })

        add(tasks.register<BuildLeveldb>("${taskName}X64") {
            onlyIf { OperatingSystem.current().isWindows }
            windowsCmakeName = "MinGW Makefiles"
            debug = !release
            shared = isShared
            cCompiler = "C:\\ProgramData\\Chocolatey\\bin\\gcc.exe"
            cxxCompiler = "C:\\ProgramData\\Chocolatey\\bin\\g++.exe"
            cxxFlags = flags
            outputDir(leveldbBuildDir.map { it.dir("$dirPath/x64") }, "libleveldb.dll")
            sourcesDir = layout.projectDirectory.dir("leveldb")
        })
    }
}

val buildLeveldbWindows by tasks.registering {
    dependsOn(winTasks)
}

// Linux
val linuxTasks = buildList<Any> {
//    matrix.forEach { (release, isShared) ->
        val buildLeveldbSharedLinuxX64 by tasks.registering(BuildLeveldb::class) {
            onlyIf { OperatingSystem.current().isLinux }
            // debug = TODO()
            shared = true
            cCompiler = "gcc-9"
            cxxCompiler = "g++-9"
            cxxFlags = listOf("-static-libgcc", "-static-libstdc++")
            outputDir(leveldbBuildDir.map { it.dir("linux/shared/x64") }, "libleveldb.so")
            sourcesDir = layout.projectDirectory.dir("leveldb")
        }

        val buildLeveldbStaticLinuxX64 by tasks.registering(BuildLeveldb::class) {
            onlyIf { OperatingSystem.current().isLinux }
            // debug = TODO()
            shared = false
            cCompiler = "gcc-9"
            cxxCompiler = "g++-9"
            outputDir(leveldbBuildDir.map { it.dir("linux/static/x64") }, "libleveldb.a")
            sourcesDir = layout.projectDirectory.dir("leveldb")
        }

        val buildLeveldbSharedLinuxArm64 by tasks.registering(BuildLeveldb::class) {
            onlyIf { OperatingSystem.current().isLinux }
            // debug = TODO()
            shared = true
            cCompiler = "aarch64-linux-gnu-gcc-9"
            cxxCompiler = "aarch64-linux-gnu-g++-9"
            cxxFlags = listOf("-static-libgcc", "-static-libstdc++")
            outputDir(leveldbBuildDir.map { it.dir("linux/shared/arm64") }, "libleveldb.so")
            sourcesDir = layout.projectDirectory.dir("leveldb")
        }

        val buildLeveldbStaticLinuxArm64 by tasks.registering(BuildLeveldb::class) {
            onlyIf { OperatingSystem.current().isLinux }
            // debug = TODO()
            shared = false
            cCompiler = "aarch64-linux-gnu-gcc-9"
            cxxCompiler = "aarch64-linux-gnu-g++-9"
            outputDir(leveldbBuildDir.map { it.dir("linux/static/arm64") }, "libleveldb.a")
            sourcesDir = layout.projectDirectory.dir("leveldb")
        }

        val buildLeveldbSharedLinuxArmV7a by tasks.registering(BuildLeveldb::class) {
            onlyIf { OperatingSystem.current().isLinux }
            // debug = TODO()
            shared = true
            cCompiler = "arm-linux-gnueabihf-gcc-9"
            cxxCompiler = "arm-linux-gnueabihf-g++-9"
            cxxFlags = listOf("-static-libgcc", "-static-libstdc++")
            outputDir(leveldbBuildDir.map { it.dir("linux/shared/armv7a") }, "libleveldb.so")
            sourcesDir = layout.projectDirectory.dir("leveldb")
        }

        val buildLeveldbLinux by tasks.registering {
            dependsOn(
                buildLeveldbSharedLinuxX64,
                buildLeveldbStaticLinuxX64,
                buildLeveldbSharedLinuxArm64,
                buildLeveldbStaticLinuxArm64,
                buildLeveldbSharedLinuxArmV7a
            )
        }

// Android
        val buildLeveldbSharedAndroidArm64 by tasks.registering(BuildLeveldb::class) {
            onlyIf { OperatingSystem.current().isLinux }
            systemName = "Android"
            androidNdkPath = findAndroidNdk()?.absolutePathString()
            systemVersion = 35
            androidStlType = "c++_static"
            androidAbi = "arm64-v8a"
            shared = true
            // debug = TODO()
            outputDir(leveldbBuildDir.map { it.dir("android/shared/arm64") }, "libleveldb.so")
            sourcesDir = layout.projectDirectory.dir("leveldb")
        }

        val buildLeveldbStaticAndroidArm64 by tasks.registering(BuildLeveldb::class) {
            onlyIf { OperatingSystem.current().isLinux }
            systemName = "Android"
            androidNdkPath = findAndroidNdk()?.absolutePathString()
            systemVersion = 35
            androidStlType = "c++_shared"
            androidAbi = "arm64-v8a"
            shared = false
            // debug = TODO()
            outputDir(leveldbBuildDir.map { it.dir("android/static/arm64") }, "libleveldb.a")
            sourcesDir = layout.projectDirectory.dir("leveldb")
        }

        val buildLeveldbSharedAndroidArmV7a by tasks.registering(BuildLeveldb::class) {
            onlyIf { OperatingSystem.current().isLinux }
            systemName = "Android"
            androidNdkPath = findAndroidNdk()?.absolutePathString()
            systemVersion = 35
            androidStlType = "c++_static"
            androidAbi = "armeabi-v7a"
            shared = true
            // debug = TODO()
            outputDir(leveldbBuildDir.map { it.dir("android/shared/armv7a") }, "libleveldb.so")
            sourcesDir = layout.projectDirectory.dir("leveldb")
        }

        val buildLeveldbStaticAndroidArmV7a by tasks.registering(BuildLeveldb::class) {
            onlyIf { OperatingSystem.current().isLinux }
            systemName = "Android"
            androidNdkPath = findAndroidNdk()?.absolutePathString()
            systemVersion = 35
            androidStlType = "c++_shared"
            androidAbi = "armeabi-v7a"
            shared = false
            // debug = TODO()
            outputDir(leveldbBuildDir.map { it.dir("android/static/armv7a") }, "libleveldb.a")
            sourcesDir = layout.projectDirectory.dir("leveldb")
        }

        val buildLeveldbSharedAndroidX86 by tasks.registering(BuildLeveldb::class) {
            onlyIf { OperatingSystem.current().isLinux }
            systemName = "Android"
            androidNdkPath = findAndroidNdk()?.absolutePathString()
            systemVersion = 35
            androidStlType = "c++_static"
            androidAbi = "x86"
            shared = true
            // debug = TODO()
            outputDir(leveldbBuildDir.map { it.dir("android/shared/x86") }, "libleveldb.so")
            sourcesDir = layout.projectDirectory.dir("leveldb")
        }

        val buildLeveldbStaticAndroidX86 by tasks.registering(BuildLeveldb::class) {
            onlyIf { OperatingSystem.current().isLinux }
            systemName = "Android"
            androidNdkPath = findAndroidNdk()?.absolutePathString()
            systemVersion = 35
            androidStlType = "c++_shared"
            androidAbi = "x86"
            shared = false
            // debug = TODO()
            outputDir(leveldbBuildDir.map { it.dir("android/static/x86") }, "libleveldb.a")
            sourcesDir = layout.projectDirectory.dir("leveldb")
        }

        val buildLeveldbSharedAndroidX64 by tasks.registering(BuildLeveldb::class) {
            onlyIf { OperatingSystem.current().isLinux }
            systemName = "Android"
            androidNdkPath = findAndroidNdk()?.absolutePathString()
            systemVersion = 35
            androidStlType = "c++_static"
            androidAbi = "x86_64"
            shared = true
            // debug = TODO()
            outputDir(leveldbBuildDir.map { it.dir("android/shared/x86_64") }, "libleveldb.so")
            sourcesDir = layout.projectDirectory.dir("leveldb")
        }

        val buildLeveldbStaticAndroidX64 by tasks.registering(BuildLeveldb::class) {
            onlyIf { OperatingSystem.current().isLinux }
            systemName = "Android"
            androidNdkPath = findAndroidNdk()?.absolutePathString()
            systemVersion = 35
            androidStlType = "c++_shared"
            androidAbi = "x86_64"
            shared = false
            // debug = TODO()
            outputDir(leveldbBuildDir.map { it.dir("android/static/x86_64") }, "libleveldb.a")
            sourcesDir = layout.projectDirectory.dir("leveldb")
//        }
    }
}
val buildLeveldbAndroid by tasks.registering {
    dependsOn(
//        buildLeveldbSharedAndroidArm64,
//        buildLeveldbStaticAndroidArm64,
//        buildLeveldbSharedAndroidArmV7a,
//        buildLeveldbStaticAndroidArmV7a,
//        buildLeveldbSharedAndroidX86,
//        buildLeveldbStaticAndroidX86,
//        buildLeveldbSharedAndroidX64,
//        buildLeveldbStaticAndroidX64
    )
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

val appleTasks = buildList {
    appleTargets.forEach { (arch, sysName, sysRoot) ->
        val taskName = "${sysRoot.capitalized()}${arch.capitalized()}"
        if (sysRoot == "macosx") {
            add(tasks.register<BuildLeveldb>("buildLeveldbShared$taskName") {
                onlyIf { OperatingSystem.current().isMacOsX }
                // debug = TODO()
                shared = true
                systemName = sysName
                osxArch = arch
                osxSysroot = sysRoot
                outputDir(
                    leveldbBuildDir.map { it.dir("$sysRoot/shared/$arch") },
                    "libleveldb.dylib"
                )
                sourcesDir = layout.projectDirectory.dir("leveldb")
            })
        }
        add(tasks.register<BuildLeveldb>("buildLeveldbStatic$taskName") {
            onlyIf { OperatingSystem.current().isMacOsX }
            // debug = TODO()
            shared = false
            systemName = sysName
            osxArch = arch
            osxSysroot = sysRoot
            outputDir(leveldbBuildDir.map { it.dir("$sysRoot/static/$arch") }, "leveldb.a")
            sourcesDir = layout.projectDirectory.dir("leveldb")
        })
    }
}

val buildLeveldbApple by tasks.registering {
    dependsOn(appleTasks)
}

val buildLeveldb by tasks.registering {
    dependsOn(
//        buildLeveldbWindows,
//        buildLeveldbLinux,
//        buildLeveldbAndroid,
//        buildLeveldbApple
    )
}