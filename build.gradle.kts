plugins {
    id("com.android.application") version "9.1.0-alpha09" apply false
    id("com.android.legacy-kapt") version "9.1.0-alpha09" apply false
    id("org.jetbrains.kotlin.android") version "2.3.10" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.10" apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
