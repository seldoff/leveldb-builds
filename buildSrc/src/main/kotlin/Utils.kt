import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.registering
import java.util.Properties
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.time.Duration.Companion.days

fun String.toCamelCase() =
    split("[^A-Za-z0-9]+".toRegex())
        .joinToString("") { it.lowercase().replaceFirstChar(Char::uppercase) }
        .replaceFirstChar(Char::lowercase)

val Project.localProperties: Map<String, String>
    get() {
        val localPropertiesFile = rootProject.file("local.properties")
        if (!localPropertiesFile.exists()) {
            return emptyMap()
        }
        val p = Properties()
        localPropertiesFile
            .inputStream()
            .use { p.load(it) }
        return p.entries.associate { it.key.toString() to it.value.toString() }
    }

fun Project.findAndroidSdk() =
    getAndroidSdkPathString()?.let { Path(it) }

private fun Project.getAndroidSdkPathString(): String? =
    project.findProperty("sdk.dir") as String?
        ?: project.localProperties["sdk.dir"]
        ?: System.getenv("ANDROID_SDK_HOME")
        ?: System.getenv("ANDROID_SDK_ROOT")
        ?: System.getenv("ANDROID_SDK")

fun Project.findAndroidNdk() =
    getAndroidNdkPathString()
        ?.let { Path(it) }
        ?.takeIf { it.exists() }
        ?: findAndroidSdk()
            ?.resolve("ndk")
            ?.listDirectoryEntries()
            ?.find { it.isDirectory() }

private fun Project.getAndroidNdkPathString(): String? =
    project.findProperty("ndk.dir") as String?
        ?: project.localProperties["ndk.dir"]
        ?: System.getenv("ANDROID_NDK_HOME")
        ?: System.getenv("ANDROID_NDK_ROOT")
        ?: System.getenv("ANDROID_NDK")

fun <T> MutableList<T>.addAll(vararg elements: T) {
    addAll(elements)
}