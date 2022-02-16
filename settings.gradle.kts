pluginManagement {
    repositories {
        gradlePluginPortal()
        //mavenLocal()
        mavenCentral()
        google()
        maven("https://s01.oss.sonatype.org/content/repositories/snapshot")
    }
}
plugins {
    id("com.gradle.enterprise") version "3.8.1"
    id("de.fayard.refreshVersions") version "0.40.1"
}

// https://dev.to/jmfayard/the-one-gradle-trick-that-supersedes-all-the-others-5bpg
gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
        publishAlways()
        buildScanPublished {
            file("buildscan.log").appendText("${java.util.Date()} - $buildScanUri\n")
        }
    }

}

rootProject.name = "transkribe-cli"
