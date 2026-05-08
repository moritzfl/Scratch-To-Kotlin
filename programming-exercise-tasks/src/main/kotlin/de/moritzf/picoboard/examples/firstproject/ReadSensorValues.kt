package de.moritzf.picoboard.examples.firstproject

import de.moritzf.picoboard.easy.PicoBoardEasy

fun main() {

    PicoBoardEasy.run {
        every(100) {

            // Task 1: Try to find out how to read the built-in sensors on the PicoBoard.
            // Task 2: The PicoBoard has connectors for external sensors.
            //         - First, without a sensor, find out what happens when you hold the connector clips together.
            //         - Then try to find out what as many sensors as possible actually do.
            if (buttonPressed()) {
                println("Someone is pressing the button");
            } else {
                println("Nobody is pressing the button");
            }
        }
    }
}
