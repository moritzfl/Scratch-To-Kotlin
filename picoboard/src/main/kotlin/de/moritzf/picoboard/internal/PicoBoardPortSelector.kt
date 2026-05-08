package de.moritzf.picoboard.internal

import de.moritzf.picoboard.PicoBoardPort
import de.moritzf.picoboard.PicoBoardPortSelectionException

internal object PicoBoardPortSelector {
    internal fun autoSelect(ports: List<PicoBoardPort>): PicoBoardPort? {
        return selectScoredPort(ports)?.port
    }

    @Throws(PicoBoardPortSelectionException::class)
    internal fun requireAutoSelected(ports: List<PicoBoardPort>): PicoBoardPort {
        val best = selectScoredPort(ports)
        if (best != null) {
            return best.port
        }

        val scoredPorts = ports.map(::scorePort)
        val positiveScores = scoredPorts.filter { it.score > 0 }
        if (positiveScores.isEmpty()) {
            throw PicoBoardPortSelectionException(
                buildString {
                    append("No suitable PicoBoard serial port found")
                    if (ports.isNotEmpty()) {
                        append(". Detected ports: ")
                        append(ports.joinToString(", ") { it.systemPortPath })
                    }
                },
            )
        }

        val highestScore = positiveScores.maxOf { it.score }
        val ambiguousPorts = positiveScores.filter { it.score == highestScore }
        throw PicoBoardPortSelectionException(
            "Multiple suitable PicoBoard serial ports found: " +
                ambiguousPorts.joinToString(", ") { it.port.systemPortPath },
        )
    }

    private fun selectScoredPort(ports: List<PicoBoardPort>): ScoredPort? {
        val positiveScores = ports.map(::scorePort).filter { it.score > 0 }
        if (positiveScores.isEmpty()) {
            return null
        }

        val highestScore = positiveScores.maxOf { it.score }
        val bestPorts = positiveScores.filter { it.score == highestScore }
        return bestPorts.singleOrNull()
    }

    private fun scorePort(port: PicoBoardPort): ScoredPort {
        val haystack = listOf(
            port.systemPortPath,
            port.systemPortName,
            port.descriptivePortName,
            port.portDescription,
        ).joinToString(" ").lowercase()

        var score = 0
        score += keywordScore(haystack, "picoboard", 1_000)
        score += keywordScore(haystack, "scratchboard", 900)
        score += keywordScore(haystack, "ft232r", 800)
        score += keywordScore(haystack, "ft232", 700)
        score += keywordScore(haystack, "ftdi", 650)
        score += keywordScore(haystack, "usbserial", 500)
        score += keywordScore(haystack, "usb modem", 450)
        score += keywordScore(haystack, "usbmodem", 450)
        score += keywordScore(haystack, "usb uart", 425)
        score += keywordScore(haystack, "uart", 150)

        val path = port.systemPortPath.lowercase()
        if (path.startsWith("/dev/cu.usb")) {
            score += 325
        }
        if (path.startsWith("/dev/tty.usb")) {
            score += 250
        }
        if (path.startsWith("/dev/ttyusb") || path.startsWith("/dev/ttyacm")) {
            score += 275
        }
        if (WINDOWS_SERIAL_PORT.matches(port.systemPortPath) || WINDOWS_SERIAL_PORT.matches(port.systemPortName)) {
            score += 200
        }

        return ScoredPort(port = port, score = score)
    }

    private fun keywordScore(haystack: String, needle: String, score: Int): Int {
        return if (needle in haystack) score else 0
    }

    private data class ScoredPort(
        val port: PicoBoardPort,
        val score: Int,
    )

    private val WINDOWS_SERIAL_PORT: Regex = Regex("^com\\d+$", RegexOption.IGNORE_CASE)
}
