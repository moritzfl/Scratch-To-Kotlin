package de.moritzf.picoboard.examples.firstproject

import de.moritzf.picoboard.easy.PicoBoardEasy

fun main() {

    PicoBoardEasy.run {
        every(100) {

            // Aufgabe 1: Versuche herauszufinden, wie du die integrierten Sensoren auf dem Picoboard auslesen kannst
            // Aufgabe 2: Das Picoboard hat Anschlüsse für externe Sensoren.
            //             - Probiere zunächst ohne einen Sensor herauszufinden, was passiert, wenn du die
            //               Verbindungsklammern aneinander hälst.
            //             - Versuche anschließend für möglichst viele Sensoren herauszufinden, was diese eigentlich
            //               tun.
            if (buttonPressed()) {
                println("Jemand drückt auf den Knopf");
            } else {
                println("Niemand drückt auf den Knopf");
            }
        }
    }
}
