plugins {
    id("com.android.application")
    id("com.android.legacy-kapt")
}
android {
    val versionBase = "5.0.2"

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

        versionCode = 370
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

    flavorDimensions += "default"
    productFlavors {
        create("apkkit") {
            dimension = "default"
            resValue("string", "app_name", "牧歌App工具箱")
        }
        create("appshare") {
            dimension = "default"
            resValue("string", "app_name", "AppShare")
        }
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
        buildConfig = true
        resValues = true
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
                    if (debug.size >= 5) {
                        fileType = ".APK"
                    }
                    
                    val prefix = if (variant.name.contains("apkkit", ignoreCase = true)) "apkkit" else "appshare"
                    val newName = "$prefix-${variant.outputs.first().versionName.get()}(${variant.outputs.first().versionCode.get()})${fileType}"
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
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.2.0")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.3.0")
    implementation("com.belerweb:pinyin4j:2.5.1")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("com.github.getActivity:XXPermissions:28.0")
    implementation("androidx.documentfile:documentfile:1.1.0")

    // ViewPager2 - 现代化的页面滑动组件
    implementation("androidx.viewpager2:viewpager2:1.1.0")

    // BRV - 强大的RecyclerView框架
    implementation("com.github.liangjingkanji:BRV:1.6.1")

    // MPAndroidChart - 图表库
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
}

// 注册自动导出逻辑
// 使用 afterEvaluate 确保所有 Android 只读任务已创建
// 注册自动导出逻辑
// 使用 androidComponents API 替代旧版 API
androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        val variantName = variant.name // 例如：apkkitRelease
        val capName = variantName.replaceFirstChar { it.uppercase() } 
        // 自动推断 Flavor 名称 (移除后缀 Release)
        val flavorName = variantName.removeSuffix("Release")
        
        val exportTaskName = "export${capName}Apk"
        val packageTaskName = "package$capName"
        val outputDir = layout.buildDirectory.dir("outputs/apk/$flavorName/release")
        
        // 注册导出任务
        tasks.register<Copy>(exportTaskName) {
            description = "Export APK for $variantName"
            group = "build"
            
            from(outputDir)
            include("*.apk")
            exclude("**/*unaligned*", "**/*unsigned*")
            into(layout.projectDirectory.dir("release"))
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
            
            // 显式依赖打包任务 (通过名称引用，避免未找到任务异常)
            dependsOn(packageTaskName)
            
            doFirst {
                println("Exporting APK from: $outputDir")
            }
            doLast {
                println("Exported to: ${layout.projectDirectory.dir("release").asFile.absolutePath}")
            }
        }
        
        // 通过配置规则关联 assemble 任务
        // 使用 configureEach 确保即使 assemble 任务后创建也能生效
        project.tasks.configureEach { 
            if (name == "assemble$capName") {
                finalizedBy(exportTaskName)
            }
        }
    }
}