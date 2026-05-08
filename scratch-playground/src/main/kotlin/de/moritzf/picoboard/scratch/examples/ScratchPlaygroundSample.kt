package de.moritzf.picoboard.scratch.examples

import de.moritzf.picoboard.scratch.ScratchRotationStyle
import de.moritzf.picoboard.scratch.scratchStage
import korlibs.event.Key
import korlibs.image.color.Colors

suspend fun main(): Unit = scratchStage(width = 1000, height = 700, title = "Scratch Playground Sample") {
    val player = rectangle(
        width = 140,
        height = 24,
        color = Colors["#E2C044"],
    ) {
        goTo(0, -250)
        rotationStyle = ScratchRotationStyle.DONT_ROTATE
    }

    val ball = circle(
        radius = 16,
        color = Colors["#FF7F50"],
    ) {
        goTo(0, 40)
        pointInDirection(35)
    }

    forever {
        if (keyPressed(Key.LEFT)) {
            player.changeXBy(-6)
        }
        if (keyPressed(Key.RIGHT)) {
            player.changeXBy(6)
        }

        ball.move(6)
        ball.ifOnEdgeBounce()

        if (ball.touching(player)) {
            println("Hit")
        }
    }
}
