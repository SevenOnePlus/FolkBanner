import java.util.Properties
import org.apache.tools.ant.taskdefs.condition.Os

val minSdkValue = android.defaultConfig.minSdk ?: 24

tasks.register("buildZigNative") {
    group = "build"
    description = "Build native library with Zig"

    val zigDir = file("src/main/zig")
    val jniLibsDir = file("src/main/jniLibs")
    
    val ndkPath = project.extensions.getByType<com.android.build.gradle.BaseExtension>().ndkDirectory?.absolutePath 
        ?: System.getenv("ANDROID_NDK_HOME") 
        ?: ""

    val zigPath = System.getenv("ZIG_PATH") ?: "zig"

    outputs.dir(jniLibsDir)

    doLast {
        if (ndkPath.isEmpty() || !File(ndkPath).exists()) {
            throw GradleException("NDK path not found")
        }

        val hostOs = when {
            Os.isFamily(Os.FAMILY_WINDOWS) -> "windows-x86_64"
            Os.isFamily(Os.FAMILY_MAC) -> "darwin-x86_64"
            else -> "linux-x86_64"
        }

        val sysroot = "$ndkPath/toolchains/llvm/prebuilt/$hostOs/sysroot"

        val targetConfigs = listOf(
            Triple("arm-linux-android$minSdkValue", "arm-linux-androideabi", "armeabi-v7a"),
            Triple("aarch64-linux-android$minSdkValue", "aarch64-linux-android", "arm64-v8a"),
            Triple("i386-linux-android$minSdkValue", "i686-linux-android", "x86"),
            Triple("x86_64-linux-android$minSdkValue", "x86_64-linux-android", "x86_64")
        )

        val optimize = if (project.hasProperty("release")) "-OReleaseFast" else "-ODebug"

        targetConfigs.forEach { (zigTarget, ndkArchName, abiDirName) ->
            val outputDir = File(jniLibsDir, abiDirName)
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }

            exec {
                workingDir = zigDir
                executable = zigPath
                args(
                    "build-lib",
                    "-target", zigTarget,
                    optimize,
                    "-fPIC",
                    "-dynamic",
                    "-fno-entry",
                    "--name", "folkrandom",
                    "--sysroot", sysroot,
                    "-I$sysroot/usr/include",
                    "-I$sysroot/usr/include/$ndkArchName",
                    "-lc",
                    "jni_interface.zig",
                    "-femit-bin=${outputDir.absolutePath}/libfolkrandom.so"
                )
            }
        }
    }
}

tasks.named("preBuild") {
    dependsOn("buildZigNative")
}
