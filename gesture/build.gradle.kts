import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Pure Kotlin module: the gesture math has no Android dependencies, so it can be
// unit-tested on any computer without a watch or emulator.
plugins {
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    testImplementation(kotlin("test-junit"))
}
