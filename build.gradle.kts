plugins {
    id("com.android.application") version "9.0.0-alpha06" apply false
    id("com.android.legacy-kapt") version "9.0.0-alpha06" apply false
    id("org.jetbrains.kotlin.android") version "2.2.20" apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // 覆盖 R8 版本
        classpath("com.android.tools:r8:8.11.18")
    }
}
