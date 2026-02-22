import java.util.Properties
import org.apache.tools.ant.taskdefs.condition.Os

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.folkbanner"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.folkbanner"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
    
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.google.code.gson:gson:2.10.1")
    
    implementation("io.coil-kt:coil:2.5.0")
    
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

tasks.register("buildZigNative") {
    group = "build"
    description = "Build native library with Zig"

    val zigDir = file("src/main/zig")
    val jniLibsDir = file("src/main/jniLibs")
    val minSdkValue = 24  // 必须在 android 块之后定义，这里直接使用固定值

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
            Triple("arm-linux-androideabi", "arm-linux-androideabi", "armeabi-v7a"),
            Triple("aarch64-linux-android", "aarch64-linux-android", "arm64-v8a"),
            Triple("x86-linux-android", "i686-linux-android", "x86"),
            Triple("x86_64-linux-android", "x86_64-linux-android", "x86_64")
        )

        val optimize = if (project.hasProperty("release")) "ReleaseFast" else "Debug"

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
                    "-O", optimize,
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