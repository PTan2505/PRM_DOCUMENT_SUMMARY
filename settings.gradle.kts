pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google() // ✅ This is the correct place
        mavenCentral()
    }
}

rootProject.name = "PRM_AI"
include(":app")
