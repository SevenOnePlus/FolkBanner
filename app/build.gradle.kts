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
    val ndkPath = System.getenv("ANDROID_NDK_HOME") ?: ""
    val zigPath = System.getenv("ZIG_PATH") ?: "zig"

    outputs.dir(jniLibsDir)

    doLast {
        if (ndkPath.isEmpty()) {
            println("WARNING: ANDROID_NDK_HOME not set, skipping Zig native build")
            return@doLast
        }

        val abis = listOf(
            Triple("armeabi-v7a", "arm-linux-androideabi", "arm"),
            Triple("arm64-v8a", "aarch64-linux-android", "aarch64"),
            Triple("x86", "x86-linux-android", "i686"),
            Triple("x86_64", "x86_64-linux-android", "x86_64")
        )
        val optimize = if (project.hasProperty("release")) "ReleaseFast" else "Debug"

        val sysroot = "$ndkPath/toolchains/llvm/prebuilt/linux-x86_64/sysroot"

        for ((abi, targetTriple, arch) in abis) {
            val outputDir = File(jniLibsDir, abi)
            outputDir.mkdirs()

            exec {
                workingDir = zigDir
                commandLine(
                    zigPath, "build-lib",
                    "-target", targetTriple,
                    "-O", optimize,
                    "-fPIC",
                    "-dynamic",
                    "-fno-entry",
                    "--name", "folkrandom",
                    "-I$sysroot/usr/include",
                    "-I$sysroot/usr/include/$arch-linux-androideabi",
                    "-L$sysroot/usr/lib/$arch-linux-androideabi/24",
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
