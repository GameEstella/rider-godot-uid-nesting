plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.16.0"
}

group = providers.gradleProperty("group").get()
version = providers.gradleProperty("version").get()

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        rider("2026.1.2") {
            useInstaller = false
        }
    }
}
