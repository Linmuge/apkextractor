pluginManagement {
    repositories {
        // 添加 Google Maven 镜像（阿里云镜像为例）
        maven(url = "https://maven.google.com")

        google()
        gradlePluginPortal()
        mavenCentral()
        maven(url = "https://jitpack.io")
        maven(url = "https://maven.aliyun.com/repository/google")
        maven(url = "https://repo1.maven.org/maven2/")
        maven(url = "https://maven.aliyun.com/repository/public")
        flatDir { dirs("app/libs") }
        maven { url = uri("https://artifact.bytedance.com/repository/pangle") }
        maven { url = uri("https://maven.aliyun.com/nexus/content/repositories/releases/") }
        maven(url = "https://gitee.com/ezy/repo/raw/cosmo/")

        maven(url = "https://jfrog.takuad.com/artifactory/china_sdk")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven(url = "https://maven.google.com")

        // 添加 Google Maven 镜像（阿里云镜像为例）
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
        maven(url = "https://maven.aliyun.com/repository/google")
        maven(url = "https://repo1.maven.org/maven2/")
        maven(url = "https://maven.aliyun.com/repository/public")
        flatDir { dirs("app/libs") }
        maven { url = uri("https://artifact.bytedance.com/repository/pangle") }
        maven { url = uri("https://maven.aliyun.com/nexus/content/repositories/releases/") }
        maven(url = "https://gitee.com/ezy/repo/raw/cosmo/")

        maven(url = "https://jfrog.takuad.com/artifactory/china_sdk")
    }
}

rootProject.name = "ApkKit"
include(":app")

