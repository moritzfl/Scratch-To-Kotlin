package de.moritzf.picoboard.scratch.examples.allemeineentchen.solution

import de.moritzf.picoboard.scratch.internal.relaunchScratchMainWithModuleAccessIfNeeded
import de.moritzf.picoboard.scratch.scratchStage
import korlibs.event.Key
import korlibs.image.color.Colors
import kotlinx.coroutines.runBlocking

private const val NOTE_DURATION_SECONDS: Double = 0.35
private const val LONG_NOTE_DURATION_SECONDS: Double = 0.7

fun main(args: Array<String>) {
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
                text = "Press Space to play All My Ducklings",
                fontSize = 24,
                color = Colors["#33658A"],
            ) {
                goTo(0, -30)
                show()
            }

            fun playMelody() {
                playToneUntilDone("C", NOTE_DURATION_SECONDS)
                playToneUntilDone("D", NOTE_DURATION_SECONDS)
                playToneUntilDone("E", NOTE_DURATION_SECONDS)
                playToneUntilDone("F", NOTE_DURATION_SECONDS)
                playToneUntilDone("G", NOTE_DURATION_SECONDS)
                playToneUntilDone("G", LONG_NOTE_DURATION_SECONDS)

                playToneUntilDone("A", NOTE_DURATION_SECONDS)
                playToneUntilDone("A", NOTE_DURATION_SECONDS)
                playToneUntilDone("A", NOTE_DURATION_SECONDS)
                playToneUntilDone("A", NOTE_DURATION_SECONDS)
                playToneUntilDone("G", LONG_NOTE_DURATION_SECONDS)

                playToneUntilDone("A", NOTE_DURATION_SECONDS)
                playToneUntilDone("A", NOTE_DURATION_SECONDS)
                playToneUntilDone("A", NOTE_DURATION_SECONDS)
                playToneUntilDone("A", NOTE_DURATION_SECONDS)
                playToneUntilDone("G", LONG_NOTE_DURATION_SECONDS)

                playToneUntilDone("F", NOTE_DURATION_SECONDS)
                playToneUntilDone("F", NOTE_DURATION_SECONDS)
                playToneUntilDone("F", NOTE_DURATION_SECONDS)
                playToneUntilDone("F", NOTE_DURATION_SECONDS)
                playToneUntilDone("E", NOTE_DURATION_SECONDS)
                playToneUntilDone("E", LONG_NOTE_DURATION_SECONDS)

                playToneUntilDone("D", NOTE_DURATION_SECONDS)
                playToneUntilDone("D", NOTE_DURATION_SECONDS)
                playToneUntilDone("D", NOTE_DURATION_SECONDS)
                playToneUntilDone("D", NOTE_DURATION_SECONDS)
                playToneUntilDone("C", LONG_NOTE_DURATION_SECONDS)
            }

            forever {
                if (keyJustPressed(Key.SPACE)) {
                    playMelody()
                }
            }
        }
    }
}
