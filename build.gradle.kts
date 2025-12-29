plugins {
    id("com.android.application") version "9.1.0-alpha01" apply false
    id("com.android.legacy-kapt") version "9.1.0-alpha01" apply false
    id("org.jetbrains.kotlin.android") version "2.2.20" apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
