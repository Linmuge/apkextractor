plugins {
    id("com.android.application")
    id("com.android.legacy-kapt")
}
android {
    val versionBase = "5.0.1"

    compileSdk{
        version = release(36)
    }
    buildToolsVersion = "36.1.0"

    defaultConfig {
        applicationId = "info.muge.appshare"

        minSdk{
            version = release(24)
        }
        targetSdk{
            version = release(36)
        }

        versionCode = 364
        versionName = versionBase

        multiDexEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
        vectorDrawables {
            useSupportLibrary = true
        }

        renderscriptTargetApi = 24
        renderscriptSupportModeEnabled = true

    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
        getByName("debug") {
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = true
            applicationIdSuffix = ".kit"
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += setOf(
                "META-INF/LICENSE-LGPL-2.1.txt",
                "META-INF/LICENSE-LGPL-3.txt",
                "META-INF/LICENSE-W3C-TEST",
                "META-INF/DEPENDENCIES",
                "META-INF/androidx/emoji2/emoji2/LICENSE.txt"
            )
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
        warningsAsErrors = false
    }

    namespace = "info.muge.appshare"
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            (output as? com.android.build.api.variant.impl.VariantOutputImpl)?.let {
                if (it.outputFileName.get().endsWith(".apk")) {
                    val debug = variant.outputs.first().versionName.get().split(".")
                    var fileType = ".apk"
                    if (debug.size >= 5) { fileType = ".APK" }
                    val newName = "appkit-${variant.outputs.first().versionName.get()}(${variant.outputs.first().versionCode.get()})${fileType}"
                    output.outputFileName.set(newName)
                }
            }
        }
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.emoji2:emoji2:1.6.0") // 使用合适的版本号
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.3.0")
    implementation("com.belerweb:pinyin4j:2.5.1")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("com.github.getActivity:XXPermissions:26.5")
    implementation("androidx.documentfile:documentfile:1.1.0")

    // ViewPager2 - 现代化的页面滑动组件
    implementation("androidx.viewpager2:viewpager2:1.1.0")

    // BRV - 强大的RecyclerView框架
    implementation("com.github.liangjingkanji:BRV:1.6.1")
}