import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.intellij.platform.gradle.extensions.IntelliJPlatformExtension

plugins {
    id("java") // Required for IntelliJ plugin development
    id("org.jetbrains.kotlin.jvm") version "1.9.20"
    id("org.jetbrains.intellij.platform") version "2.0.0" // New plugin ID and version
    kotlin("plugin.serialization") version "1.9.20"
}

group = "io.github.senang"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
    google()
    intellijPlatform {
        defaultRepositories()
    }
    // Repositories for IntelliJ Platform are typically handled by the plugin itself in v2.0
    // or can be defined in settings.gradle.kts with intellijPlatform { defaultRepositories() }
}

// Configure the IntelliJ plugin settings using the new DSL
configure<IntelliJPlatformExtension> {
    pluginConfiguration {
        name.set("KotlinTestGenAI") // Set your plugin name
        version.set(project.version as String)
        description.set(project.file("README.md").readText())
        changeNotes.set("Initial release of KotlinTestGenAI.")

        ideaVersion {
            sinceBuild.set("233") // Corresponds to 2023.3
            // untilBuild.set("241.*") // Example: Compatible up to 2024.1.*
        }
    }
    // updateSinceUntilBuild is not a direct property in v2. It's handled by sinceBuild/untilBuild in pluginConfiguration.ideaVersion
    // downloadSources.set(true) // v2 downloads sources by default if needed for dependencies

    // Define the IntelliJ Platform version to build against
    ideaCommunity {
        version.set("2023.3.6")
    }

    // Dependencies on other IntelliJ plugins (e.g., bundled ones)
    bundledPlugins.add("org.jetbrains.kotlin") // Depend on the bundled Kotlin plugin
    // instrumentationTools() // If you need bytecode manipulation tools
    // pluginVerifier() // For running plugin verifier
    // testFramework(TestFrameworkType.Platform) // For platform-level test framework
}


kotlin {
    jvmToolchain(17)
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "17"
        languageVersion = "1.9"
        apiVersion = "1.9"
        freeCompilerArgs = listOf("-Xjvm-default=all", "-Xcontext-receivers")
    }
}

// Patch plugin.xml with the correct version and build numbers
// The new plugin handles this via the pluginConfiguration block mostly.
// Tasks like patchPluginXml might be different or configured within intellijPlatform { ... }
// For now, let's rely on pluginConfiguration and see build output.

dependencies {
    // For JSON serialization/deserialization in ClaudeService.kt
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // The intellijPlatform block that was here has been moved to the top-level intellijPlatform configuration.
    // The old compileOnly(intellijPlatform()) and compileOnly(kotlinPlugin()) are no longer used.
    // The top-level intellijPlatform { ... } configuration should provide necessary APIs.
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

// Configure testing with JUnit Jupiter (JUnit 5)
/*
tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
*/ 