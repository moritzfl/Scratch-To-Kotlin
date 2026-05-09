package de.moritzf.picoboard.scratch.internal

import java.awt.Component
import java.io.File
import java.lang.management.ManagementFactory
import java.lang.StackWalker.Option.RETAIN_CLASS_REFERENCE

private const val MODULE_ACCESS_RELAUNCHED_PROPERTY: String =
    "de.moritzf.picoboard.scratch.moduleAccessRelaunched"
private const val MODULE_ACCESS_HELPER_CLASS_NAME: String =
    "de.moritzf.picoboard.scratch.internal.ScratchJvmModuleAccessKt"

private val commonDesktopPackages: List<String> = listOf(
    "java.desktop/java.awt",
    "java.desktop/sun.awt",
)

private val linuxDesktopPackages: List<String> = listOf(
    "java.desktop/sun.awt.X11",
)

private val macOsDesktopPackages: List<String> = listOf(
    "java.desktop/sun.java2d.opengl",
    "java.desktop/sun.lwawt",
    "java.desktop/sun.lwawt.macosx",
    "java.desktop/com.apple.eawt",
    "java.desktop/com.apple.eawt.event",
)

private val blockedRelaunchArgs: List<String> = listOf(
    "-agentlib:jdwp=",
    "idea_rt.jar",
    "debugger-agent.jar",
)

public fun relaunchScratchMainWithModuleAccessIfNeeded(args: Array<String>) {
    relaunchScratchMainWithModuleAccessIfNeeded(
        mainClassName = currentMainClassName(),
        args = args,
    )
}

private fun relaunchScratchMainWithModuleAccessIfNeeded(
    mainClassName: String,
    args: Array<String>,
) {
    if (hasRequiredAwtModuleAccess()) {
        return
    }

    if (System.getProperty(MODULE_ACCESS_RELAUNCHED_PROPERTY) == "true") {
        throw IllegalStateException(
            "Unable to access java.awt internals needed by KorGE even after relaunch. " +
                "Start the JVM with these flags: ${requiredModuleAccessArgs().joinToString(" ")}",
        )
    }

    val runtimeArgs = ManagementFactory.getRuntimeMXBean().inputArguments
    val command = mutableListOf<String>()
    command += javaExecutablePath()
    command += runtimeArgs.filterNot(::isBlockedRelaunchArg)
    command += requiredModuleAccessArgs().filterNot(runtimeArgs::contains)
    command += "-D$MODULE_ACCESS_RELAUNCHED_PROPERTY=true"
    command += listOf("-cp", System.getProperty("java.class.path"))
    command += mainClassName
    command += args

    val exitCode = ProcessBuilder(command)
        .inheritIO()
        .start()
        .waitFor()

    System.exit(exitCode)
    throw RuntimeException("System.exit returned normally, while it was supposed to halt JVM.")
}

private fun currentMainClassName(): String {
    return StackWalker.getInstance(RETAIN_CLASS_REFERENCE).walk { frames ->
        frames
            .filter { frame -> frame.className != MODULE_ACCESS_HELPER_CLASS_NAME }
            .filter { frame -> frame.methodName == "main" }
            .findFirst()
            .orElseThrow {
                IllegalStateException("Unable to determine the current Scratch main class.")
            }
            .declaringClass
            .name
    }
}

private fun hasRequiredAwtModuleAccess(): Boolean {
    val currentModule = object {}.javaClass.module
    val desktopModule = Component::class.java.module

    val peerAccess = runCatching {
        Component::class.java.getDeclaredField("peer").trySetAccessible()
    }.getOrDefault(false)

    if (!peerAccess) {
        return false
    }

    if (!System.getProperty("os.name").contains("Linux", ignoreCase = true)) {
        return true
    }

    return desktopModule.isOpen("sun.awt.X11", currentModule) &&
        desktopModule.isExported("sun.awt.X11", currentModule)
}

private fun requiredModuleAccessArgs(): List<String> {
    val osName = System.getProperty("os.name")
    val packageNames = commonDesktopPackages + when {
        osName.contains("Mac", ignoreCase = true) -> macOsDesktopPackages
        osName.contains("Linux", ignoreCase = true) -> linuxDesktopPackages
        else -> emptyList()
    }

    return buildList {
        for (packageName in packageNames) {
            add("--add-opens=$packageName=ALL-UNNAMED")
            add("--add-exports=$packageName=ALL-UNNAMED")
        }
    }
}

private fun javaExecutablePath(): String {
    val executableName = if (File.separatorChar == '\\') "java.exe" else "java"
    return File(File(System.getProperty("java.home"), "bin"), executableName).absolutePath
}

private fun isBlockedRelaunchArg(argument: String): Boolean {
    return blockedRelaunchArgs.any(argument::contains)
}
