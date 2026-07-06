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
        // Artifex's Maven repository — hosts the MuPDF Android artifacts (com.artifex.mupdf:fitz)
        maven("https://maven.ghostscript.com")
    }
}

rootProject.name = "OpenPDF"
include(":app")
