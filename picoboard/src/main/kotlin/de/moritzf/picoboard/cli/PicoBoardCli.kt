package de.moritzf.picoboard.cli

import de.moritzf.picoboard.PicoBoard
import de.moritzf.picoboard.PicoBoardException
import de.moritzf.picoboard.PicoBoardFrame
import de.moritzf.picoboard.PicoBoardPort
import de.moritzf.picoboard.PicoBoardPortSelectionException
import java.time.Duration
import kotlin.system.exitProcess

public fun main(args: Array<String>) {
    val options = CliOptions.parse(args) ?: run {
        printUsage()
        exitProcess(2)
    }

    if (options.showHelp) {
        printUsage()
        return
    }

    if (options.listPortsOnly) {
        printPorts()
        return
    }

    val port = if (options.portPath != null) {
        findPortByPath(options.portPath) ?: run {
            System.err.println("Serial port '${options.portPath}' was not found.")
            exitProcess(1)
        }
    } else {
        try {
            PicoBoard.requireAutoSelectedPort()
        } catch (failure: PicoBoardPortSelectionException) {
            System.err.println("${failure.message}. Use --list-ports or --port <path>.")
            exitProcess(1)
        }
    }

    try {
        PicoBoard.open(
            port = port,
            options = de.moritzf.picoboard.PicoBoardOptions(
                readTimeout = options.readTimeout,
                pollingInterval = Duration.ofMillis(options.intervalMillis),
            ),
        ).use { board ->
            println("Connected to ${port.systemPortPath}")
            println("Press Ctrl+C to stop.")

            var remaining = options.count
            var consecutiveReadFailures = 0
            val maxReadRetries = board.options.pollingReadFailureRetries
            while (true) {
                val frame = try {
                    board.readFrame().also {
                        consecutiveReadFailures = 0
                    }
                } catch (failure: PicoBoardException) {
                    if (remaining == 0 && consecutiveReadFailures < maxReadRetries) {
                        consecutiveReadFailures += 1
                        System.err.println(
                            "Read failed during continuous polling on ${port.systemPortPath} " +
                                "(${consecutiveReadFailures}/$maxReadRetries retries): ${failure.message}",
                        )
                        if (options.intervalMillis > 0L) {
                            Thread.sleep(options.intervalMillis)
                        }
                        continue
                    }

                    if (remaining == 0) {
                        throw PicoBoardException(
                            "Continuous polling failed after $maxReadRetries read retries on '${port.systemPortPath}'",
                            failure,
                        )
                    }

                    throw failure
                }
                println(frame.formatLine())

                if (remaining > 0) {
                    remaining -= 1
                    if (remaining == 0) {
                        break
                    }
                }

                if (options.intervalMillis > 0L) {
                    Thread.sleep(options.intervalMillis)
                }
            }
        }
    } catch (failure: PicoBoardException) {
        System.err.println("Failed to communicate with ${port.systemPortPath}: ${failure.message}")
        exitProcess(1)
    }
}

private data class CliOptions(
    val portPath: String?,
    val count: Int,
    val intervalMillis: Long,
    val readTimeout: Duration,
    val listPortsOnly: Boolean,
    val showHelp: Boolean,
) {
    companion object {
        fun parse(args: Array<String>): CliOptions? {
            var portPath: String? = null
            var count = 0
            var intervalMillis = 100L
            var readTimeout = Duration.ofMillis(750)
            var listPortsOnly = false
            var showHelp = false

            var index = 0
            while (index < args.size) {
                when (val argument = args[index]) {
                    "--help", "-h" -> showHelp = true
                    "--list-ports" -> listPortsOnly = true
                    "--port" -> {
                        index += 1
                        portPath = args.getOrNull(index) ?: return null
                    }
                    "--count" -> {
                        index += 1
                        count = args.getOrNull(index)?.toIntOrNull() ?: return null
                    }
                    "--interval-ms" -> {
                        index += 1
                        intervalMillis = args.getOrNull(index)?.toLongOrNull() ?: return null
                    }
                    "--timeout-ms" -> {
                        index += 1
                        val timeoutMillis = args.getOrNull(index)?.toLongOrNull() ?: return null
                        readTimeout = Duration.ofMillis(timeoutMillis)
                    }
                    else -> {
                        if (argument.startsWith("--")) {
                            return null
                        }
                        return null
                    }
                }
                index += 1
            }

            if (count < 0 || intervalMillis < 0L || readTimeout.isNegative || readTimeout.isZero) {
                return null
            }

            return CliOptions(
                portPath = portPath,
                count = count,
                intervalMillis = intervalMillis,
                readTimeout = readTimeout,
                listPortsOnly = listPortsOnly,
                showHelp = showHelp,
            )
        }
    }
}

private fun printUsage(): Unit {
    println(
        """
        Usage: ./gradlew run --args="[--port <path>] [--count N] [--interval-ms N] [--timeout-ms N]"

        Options:
          --list-ports           Print detected serial ports
          --port <path>          Serial device path. If omitted, the library auto-selects a suitable port.
          --count <n>            Number of frames to read, default 0 for continuous mode
          --interval-ms <n>      Delay between reads, default 100
          --timeout-ms <n>       Read timeout per poll, default 750
          --help, -h             Show this help
        """.trimIndent(),
    )
}

private fun printPorts(): Unit {
    val ports = PicoBoard.listPorts()
    if (ports.isEmpty()) {
        println("No serial ports found.")
        return
    }

    ports.forEach { port ->
        println("${port.systemPortPath} | ${port.systemPortName} | ${port.descriptivePortName} | ${port.portDescription}")
    }
}

private fun findPortByPath(path: String): PicoBoardPort? {
    return PicoBoard.listPorts().firstOrNull { it.systemPortPath == path }
}

private fun PicoBoardFrame.formatLine(): String {
    return buildString {
        append(timestamp)
        append(" firmware=")
        append(firmwareId)
        append(" raw[")
        append("slider=").append(raw.slider)
        append(", light=").append(raw.light)
        append(", sound=").append(raw.sound)
        append(", button=").append(raw.button)
        append(", A=").append(raw.resistanceA)
        append(", B=").append(raw.resistanceB)
        append(", C=").append(raw.resistanceC)
        append(", D=").append(raw.resistanceD)
        append("] scaled[")
        append("slider=").append(scaled.slider)
        append(", light=").append(scaled.light)
        append(", sound=").append(scaled.sound)
        append(", buttonPressed=").append(scaled.buttonPressed)
        append(", A=").append(scaled.resistanceA)
        append(", B=").append(scaled.resistanceB)
        append(", C=").append(scaled.resistanceC)
        append(", D=").append(scaled.resistanceD)
        append(']')
    }
}
