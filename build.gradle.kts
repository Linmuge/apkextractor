plugins {
    id("com.android.application") version "9.2.0-alpha02" apply false
    id("org.jetbrains.kotlin.android") version "2.3.10" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.10" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.10" apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
