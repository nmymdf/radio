pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google(); mavenCentral()
        // YouTube 自帶網頁引擎（Firefox GeckoView）
        maven { url = uri("https://maven.mozilla.org/maven2/") }
    }
}
rootProject.name = "carhome"
include(":app")
