pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.juspay.in/jp-build-packages/hyper-sdk/")
        }
        maven {
            url = uri("https://jitpack.io")
            content {
                excludeGroup("io.github.boltearth") // keep JitPack away from bolt-earth
            }
        }
        maven {
            url = uri("https://raw.githubusercontent.com/BOLT-EARTH/bolt-earth-android-sdk/main/releases")
            // no content filter — let it resolve freely
        }
    }
}

rootProject.name = "testinguisdkgithub"
include(":app")
 