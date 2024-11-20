import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

open class BuildLeveldb @Inject constructor(
    objectFactory: ObjectFactory
) : DefaultTask() {

    init {
        group = "leveldb"
        description = "Compile LevelDB"
    }

    @get:Input
    @get:Optional
    val windowsCmakeName = objectFactory.property<String>()

    @get:Input
    val cmakePath = objectFactory.property<String>()
        .convention("cmake")

    /**
     * -DLEVELDB_BUILD_TESTS
     */
    @get:Input
    val withTests = objectFactory.property<Boolean>()
        .convention(false)

    /**
     * -DLEVELDB_BUILD_BENCHMARKS
     */
    @get:Input
    val withBenchmarks = objectFactory.property<Boolean>()
        .convention(false)

    /**
     * -DBUILD_SHARED_LIBS
     */
    @get:Input
    val shared = objectFactory.property<Boolean>()

    /**
     * -DCMAKE_C_COMPILER
     */
    @get:Input
    val cCompiler = objectFactory.property<String>()
        .convention("gcc")

    /**
     * -DCMAKE_CXX_COMPILER
     */
    @get:Input
    val cxxCompiler = objectFactory.property<String>()
        .convention("g++")

    @get:Input
    @get:Optional
    val systemProcessorName = objectFactory.property<String>()

    /**
     * -DCMAKE_CXX_FLAGS
     */
    @get:Input
    val cxxFlags = objectFactory.listProperty<String>()

    /**
     * -DCMAKE_C_FLAGS
     */
    @get:Input
    val cFlags = objectFactory.listProperty<String>()

    @get:Input
    val debug = objectFactory.property<Boolean>()
        .convention(false)

    @get:InputDirectory
    val sourcesDir = objectFactory.directoryProperty()

    @get:OutputDirectory
    val outputDir = objectFactory.directoryProperty()

    @get:OutputFile
    val outputArtifact = objectFactory.fileProperty()

    @get:Input
    @get:Optional
    val systemName = objectFactory.property<String>()

    @get:Input
    @get:Optional
    val osxArch = objectFactory.property<String>()

    @get:Input
    @get:Optional
    val osxSysroot = objectFactory.property<String>()

    @get:Input
    @get:Optional
    val androidAbi = objectFactory.property<String>()

    @get:Input
    @get:Optional
    val androidNdkPath = objectFactory.property<String>()

    @get:Input
    @get:Optional
    val systemVersion = objectFactory.property<Int>()

    @get:Input
    @get:Optional
    val androidStlType = objectFactory.property<String>()

    fun outputDir(dir: Provider<Directory>, artifactName: String){
        outputDir = dir
        outputArtifact = dir.map { it.file(artifactName) }
    }

    @TaskAction
    fun compile() {
        val cmakeCommand = getCmakeCommand()
        project.exec {
            standardOutput = System.out
            errorOutput = System.err
            executable = cmakePath.get()
            args = cmakeCommand

            logger.lifecycle("Executing ${commandLine.joinToString(" ")}")
        }
        val makeCommand = buildList {
            add("--build")
            add(outputDir.get().asFile.absolutePath)
            add("-j${Runtime.getRuntime().availableProcessors()}")
        }
        project.exec {
            executable = cmakePath.get()
            args = makeCommand
            logger.lifecycle("Executing ${commandLine.joinToString(" ")}")
        }
    }

    private fun getCmakeCommand() = buildList {
        windowsCmakeName.orNull
            ?.let { addAll("-G", it) }
        add("-DLEVELDB_BUILD_TESTS=${withTests.get().asString()}")
        add("-DCMAKE_BUILD_TYPE=${if (debug.get()) "Debug" else "Release"}")
        add("-DLEVELDB_BUILD_BENCHMARKS=${withBenchmarks.get().asString()}")
        systemName.orNull?.let { add("-DCMAKE_SYSTEM_NAME=$it") }
        osxArch.orNull?.let { add("-DCMAKE_OSX_ARCHITECTURES=$it") }
        osxSysroot.orNull?.let { add("-DCMAKE_OSX_SYSROOT=$it") }
        androidAbi.orNull?.let { add("-DCMAKE_ANDROID_ARCH_ABI=$it") }
        androidNdkPath.orNull?.let { add("-DCMAKE_ANDROID_NDK=$it") }
        systemVersion.orNull?.let { add("-DCMAKE_SYSTEM_VERSION=$it") }
        androidStlType.orNull?.let { add("-DCMAKE_ANDROID_STL_TYPE=$it") }
        add("-DBUILD_SHARED_LIBS=${shared.get().asString()}")
        add("-DCMAKE_C_COMPILER=${cCompiler.get()}")
        add("-DCMAKE_CXX_COMPILER=${cxxCompiler.get()}")
        systemProcessorName.orNull?.let { add("-DCMAKE_SYSTEM_PROCESSOR=$it") }
        cxxFlags.get()
            .takeIf { it.isNotEmpty() }
            ?.let { flags -> flags.joinToString(" ") }
            ?.also { add("-DCMAKE_CXX_FLAGS=$it") }

        cFlags.get()
            .takeIf { it.isNotEmpty() }
            ?.let { flags -> flags.joinToString(" ") }
            ?.also { add("-DCMAKE_C_FLAGS=${it.quoted()}") }
        add("-B")
        add(outputDir.get().asFile.absolutePath)
        add("-S")
        add(sourcesDir.get().asFile.absolutePath)
    }

}

private fun String.quoted() = "\"$this\""

private fun Boolean.asString() =
    if (this) "ON" else "OFF"

fun <T, R, K : Any> combine(
    first: Provider<T>,
    second: Provider<R>,
    block: (T, R) -> K
): Provider<K> = first.flatMap { firstValue: T ->
    second.map { secondValue: R ->
        block(firstValue, secondValue)
    }
}
