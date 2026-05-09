package de.moritzf.picoboard.scratch.examples.allemeineentchen.solution

import de.moritzf.picoboard.scratch.internal.relaunchScratchMainWithModuleAccessIfNeeded
import de.moritzf.picoboard.scratch.scratchStage
import korlibs.image.color.Colors
import kotlinx.coroutines.runBlocking

private const val NOTE_DURATION_SECONDS: Double = 0.35
private const val LONG_NOTE_DURATION_SECONDS: Double = 0.7

fun main(args: Array<String>): Unit {
    relaunchScratchMainWithModuleAccessIfNeeded(args)

    runBlocking {
        println("Starting Alle meine Entchen solution.")

        scratchStage(
            width = 800,
            height = 400,
            title = "Alle meine Entchen Solution",
            backgroundColor = Colors["#F5F1E8"],
        ) {
            text(
                text = "Alle meine Entchen",
                fontSize = 42,
                color = Colors["#2F4858"],
            ) {
                goTo(0, 50)
                show()
            }

            text(
                text = "All My Ducklings",
                fontSize = 24,
                color = Colors["#33658A"],
            ) {
                goTo(0, -30)
                show()
            }

            val melody = listOf(
                "C", "D", "E", "F", "G", "G",
                "A", "A", "A", "A", "G",
                "A", "A", "A", "A", "G",
                "F", "F", "F", "F", "E", "E",
                "D", "D", "D", "D", "C",
            )
            val longNoteIndexes = listOf(5, 10, 15, 21, 26)

            melody.forEachIndexed { index, note ->
                val isLastNoteOfLine = index in longNoteIndexes
                val duration = if (isLastNoteOfLine) LONG_NOTE_DURATION_SECONDS else NOTE_DURATION_SECONDS
                playToneUntilDone(note, duration)
            }
        }
    }
}
