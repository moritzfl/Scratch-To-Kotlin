package de.moritzf.picoboard.scratch.examples.catchthefallingball

import de.moritzf.picoboard.easy.PicoBoardEasy
import de.moritzf.picoboard.scratch.ScratchRotationStyle
import de.moritzf.picoboard.scratch.scratchStage
import de.moritzf.picoboard.scratch.internal.relaunchScratchMainWithModuleAccessIfNeeded
import korlibs.image.color.Colors
import kotlinx.coroutines.runBlocking

private const val STAGE_WIDTH: Int = 1000
private const val STAGE_HEIGHT: Int = 700

private const val GAME_LOOP_INTERVAL_MILLIS: Long = 16L
private const val PICOBOARD_POLL_INTERVAL_MILLIS: Long = GAME_LOOP_INTERVAL_MILLIS / 2

fun main(args: Array<String>): Unit {
    relaunchScratchMainWithModuleAccessIfNeeded(args)

    runBlocking {
        println("Starting Catch The Falling Ball.")
        println("Keyboard fallback: Left/Right to move, Space to start or restart.")

        var picoService = runCatching {
            PicoBoardEasy.startService(intervalMillis = PICOBOARD_POLL_INTERVAL_MILLIS)
        }.onSuccess {
            println(
                "Connected to PicoBoard on '${it.portIdentifier}'. " +
                    "Use the slider to move and the button to start or restart.",
            )
        }.onFailure { failure ->
            println(
                "No PicoBoard was auto-selected (${failure.message ?: failure::class.simpleName}). " +
                    "Using keyboard controls.",
            )
        }.getOrNull()

        scratchStage(
            width = STAGE_WIDTH,
            height = STAGE_HEIGHT,
            title = "Catch The Falling Ball",
            backgroundColor = Colors.DARKSLATEGRAY,
        ) {
            onClose {
                picoService?.close()
            }

            val catcher = rectangle(
                width = 190,
                height = 26,
                color = Colors.GOLD,
            ) {
                goTo(0, -285)
                rotationStyle = ScratchRotationStyle.DONT_ROTATE
            }

            val ball = circle(
                radius = 20,
                color = Colors.CORAL,
            ) {
                goTo(0, 285)
                rotationStyle = ScratchRotationStyle.DONT_ROTATE
            }

            forever {
                val service = picoService
                if (service != null && !service.isRunning()) {
                    val failure = service.failure()
                    println(
                        "PicoBoard polling stopped (${failure?.message ?: "unknown reason"}). " +
                            "Using keyboard controls.",
                    )
                    service.close()
                    picoService = null
                }

                // Task 1:
                // Move the catcher left and right.
                //
                // Task 2:
                // Make the ball fall down.
                //
                // Task 3:
                // When the ball touches the catcher, make the ball appear at the top again.
                //
                // Task 4:
                // Keep track of how many balls were caught.

                // Implement the game logic here.
            }
        }
    }
}
