import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.tasks.JavaExec

plugins {
    kotlin("jvm") version "2.3.0"
    `java-library`
}

val commonDesktopJdkPackages = listOf(
    "java.desktop/java.awt",
    "java.desktop/sun.awt",
)

val linuxJdkPackages = listOf(
    "java.desktop/sun.awt.X11",
)

val macOsJdkPackages = listOf(
    "java.desktop/sun.java2d.opengl",
    "java.desktop/sun.lwawt",
    "java.desktop/sun.lwawt.macosx",
    "java.desktop/com.apple.eawt",
    "java.desktop/com.apple.eawt.event",
)

fun moduleAccessArgs(packageNames: List<String>): List<String> = buildList {
    for (packageName in packageNames) {
        add("--add-opens=$packageName=ALL-UNNAMED")
        add("--add-exports=$packageName=ALL-UNNAMED")
    }
}

val osName = System.getProperty("os.name")
val scratchPlaygroundJvmArgs = moduleAccessArgs(
    commonDesktopJdkPackages + when {
        osName.contains("Mac", ignoreCase = true) -> macOsJdkPackages
        osName.contains("Linux", ignoreCase = true) -> linuxJdkPackages
        else -> emptyList()
    },
)

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

kotlin {
    jvmToolchain(21)

    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

val scratchPlaygroundLauncher = javaToolchains.launcherFor {
    languageVersion = JavaLanguageVersion.of(21)
}

dependencies {
    api("com.soywiz.korge:korge-jvm:6.0.0")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaExec>().configureEach {
    javaLauncher.set(scratchPlaygroundLauncher)
    jvmArgs(scratchPlaygroundJvmArgs)
}
