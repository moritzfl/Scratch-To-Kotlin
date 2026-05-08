import org.gradle.api.tasks.JavaExec
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
}

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

val exerciseTaskJavaLauncher = javaToolchains.launcherFor {
    languageVersion = JavaLanguageVersion.of(21)
}

dependencies {
    implementation(project(":picoboard"))
    implementation(project(":scratch-playground"))

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaExec>().configureEach {
    javaLauncher.set(exerciseTaskJavaLauncher)
    jvmArgs(scratchPlaygroundJvmArgs())
}

tasks.register<JavaExec>("readSensorValues") {
    group = "application"
    description = "Runs the beginner PicoBoard sensor values exercise."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("de.moritzf.picoboard.examples.firstproject.ReadSensorValuesKt")
}

tasks.register<JavaExec>("runCatchTheFallingBall") {
    group = "application"
    description = "Runs the Scratch-style Catch The Falling Ball starter."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("de.moritzf.picoboard.scratch.examples.catchthefallingball.CatchTheFallingBallKt")
}

fun scratchPlaygroundJvmArgs(): List<String> {
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
    val osName = System.getProperty("os.name")
    val packageNames = commonDesktopJdkPackages + when {
        osName.contains("Mac", ignoreCase = true) -> macOsJdkPackages
        osName.contains("Linux", ignoreCase = true) -> linuxJdkPackages
        else -> emptyList()
    }

    return buildList {
        for (packageName in packageNames) {
            add("--add-opens=$packageName=ALL-UNNAMED")
            add("--add-exports=$packageName=ALL-UNNAMED")
        }
    }
}
