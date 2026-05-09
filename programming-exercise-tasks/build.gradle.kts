import org.gradle.api.tasks.JavaExec
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.OutputStream

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
    errorOutput = FilteringLineOutputStream(System.err, ::isIgnoredMacOsGraphicsWarning)
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

tasks.register<JavaExec>("runAlleMeineEntchen") {
    group = "application"
    description = "Runs the Scratch-style Alle meine Entchen music exercise."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("de.moritzf.picoboard.scratch.examples.allemeineentchen.AlleMeineEntchenKt")
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
    val macOsJvmProperties = if (osName.contains("Mac", ignoreCase = true)) {
        listOf(
            "-Dapple.awt.application.name=PicoBoard Scratch Playground",
            "-Dapple.awt.application.appearance=system",
            "-Dsun.java2d.opengl=false",
            "-Dsun.java2d.metal=true",
        )
    } else {
        emptyList()
    }
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
        addAll(macOsJvmProperties)
    }
}

fun isIgnoredMacOsGraphicsWarning(line: String): Boolean {
    return line.contains("ApplePersistenceIgnoreState: Existing state will not be touched") ||
        line.contains("GLD_TEXTURE_INDEX_2D is unloadable and bound to sampler type")
}

class FilteringLineOutputStream(
    private val output: OutputStream,
    private val shouldDropLine: (String) -> Boolean,
) : OutputStream() {
    private val buffer = StringBuilder()

    override fun write(value: Int): Unit {
        val byteValue = value and 0xff
        if (byteValue == '\n'.code) {
            flushLine(includeNewline = true)
            return
        }
        buffer.append(byteValue.toChar())
    }

    override fun flush(): Unit {
        flushLine(includeNewline = false)
        output.flush()
    }

    override fun close(): Unit {
        flush()
    }

    private fun flushLine(includeNewline: Boolean): Unit {
        if (buffer.isEmpty()) {
            if (includeNewline) {
                output.write('\n'.code)
            }
            return
        }

        val line = buffer.toString()
        buffer.clear()
        if (shouldDropLine(line)) {
            return
        }

        output.write(line.toByteArray())
        if (includeNewline) {
            output.write('\n'.code)
        }
    }
}
