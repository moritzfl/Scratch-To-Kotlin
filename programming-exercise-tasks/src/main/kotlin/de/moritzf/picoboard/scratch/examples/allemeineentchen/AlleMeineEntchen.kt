package de.moritzf.picoboard.scratch.examples.allemeineentchen

import de.moritzf.picoboard.scratch.internal.relaunchScratchMainWithModuleAccessIfNeeded
import de.moritzf.picoboard.scratch.scratchStage
import korlibs.image.color.Colors
import kotlinx.coroutines.runBlocking

private const val NOTE_DURATION_SECONDS: Double = 0.35
private const val LONG_NOTE_DURATION_SECONDS: Double = 0.7

fun main(args: Array<String>): Unit {
    relaunchScratchMainWithModuleAccessIfNeeded(args)

    runBlocking {
        println("Starting Alle meine Entchen.")
        println("Task: use playToneUntilDone(...) to play the full melody.")

        scratchStage(
            width = 800,
            height = 400,
            title = "Alle meine Entchen",
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
                text = "Fill in the melody with playToneUntilDone(note, seconds).",
                fontSize = 24,
                color = Colors["#33658A"],
            ) {
                goTo(0, -30)
                show()
            }

            // Task:
            // Play "Alle meine Entchen" with generated tones.
            // The English title is usually translated as "All My Ducklings".
            //
            // Useful examples:
            // playToneUntilDone("C", NOTE_DURATION_SECONDS)
            // playToneUntilDone("D", NOTE_DURATION_SECONDS)
            // playToneUntilDone("C", LONG_NOTE_DURATION_SECONDS)
            //
            // The melody starts like this:
            // C D E F G G
            playToneUntilDone("C", NOTE_DURATION_SECONDS)
            playToneUntilDone("D", NOTE_DURATION_SECONDS)

            // Continue the melody here.
            // Try to add the remaining notes by yourself.
        }
    }
}
