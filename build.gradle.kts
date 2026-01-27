plugins {
    id("com.android.application") version "9.1.0-alpha06" apply false
    id("com.android.legacy-kapt") version "9.1.0-alpha06" apply false
    id("org.jetbrains.kotlin.android") version "2.3.0" apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
