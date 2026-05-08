import org.gradle.api.tasks.JavaExec
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    `java-library`
    application
    `maven-publish`
}

group = "de.moritzf.picoboard"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    withSourcesJar()
    withJavadocJar()
}

kotlin {
    explicitApi()
    jvmToolchain(21)

    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

val picoboardJavaLauncher = javaToolchains.launcherFor {
    languageVersion = JavaLanguageVersion.of(21)
}

dependencies {
    implementation("com.fazecast:jSerialComm:2.11.4")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<Jar>().configureEach {
    manifest {
        attributes["Automatic-Module-Name"] = "de.moritzf.picoboard"
    }
}

tasks.withType<JavaExec>().configureEach {
    javaLauncher.set(picoboardJavaLauncher)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name.set("picoboard")
                description.set("Kotlin/JVM library for using a PicoBoard from Kotlin and Java.")
            }
        }
    }
}

application {
    mainClass.set("de.moritzf.picoboard.cli.PicoBoardCliKt")
}
