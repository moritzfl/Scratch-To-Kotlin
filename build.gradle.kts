plugins {
    kotlin("jvm") version "2.3.0" apply false
}

tasks.register("readSensorValues") {
    group = "application"
    description = "Runs the beginner PicoBoard sensor values exercise."
    dependsOn(":programming-exercise-tasks:readSensorValues")
}

tasks.register("runCatchTheFallingBall") {
    group = "application"
    description = "Runs the Scratch-style Catch The Falling Ball starter."
    dependsOn(":programming-exercise-tasks:runCatchTheFallingBall")
}

tasks.register("runCatchTheFallingBallSolution") {
    group = "application"
    description = "Runs the full Scratch-style Catch The Falling Ball solution."
    dependsOn(":solutions:runCatchTheFallingBallSolution")
}
