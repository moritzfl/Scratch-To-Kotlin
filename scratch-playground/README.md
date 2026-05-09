# scratch-playground

`scratch-playground` is an optional KorGE-based module that provides a Scratch-shaped 2D API.

This module is built and run with the repository-wide Java 21 toolchain.

It is meant as the next step after the beginner PicoBoard examples:

- students still read PicoBoard values with the easy API
- they get a stage and simple ready-to-use objects similar to Scratch sprites
- they can build small games without dealing with low-level rendering setup first

## What It Provides

- `scratchStage(width, height, ...)` for a logical stage with a resizable window
- centered Scratch-like coordinates:
  `x = 0`, `y = 0` is the middle of the stage
- simple shapes:
  `rectangle(...)` and `circle(...)`
- sounds loaded from resources with `sound(...)`
- generated tones with `playTone(...)` or `playToneUntilDone(...)`
- sprite-style properties:
  `x`, `y`, `direction`, `size`, `scale`, `rotationStyle`, `visible`
- sprite-style helpers:
  `goTo(...)`, `move(...)`, `turnLeft(...)`, `turnRight(...)`, `touching(...)`, `touchingEdge()`, `ifOnEdgeBounce()`
- frame loops:
  `forever { ... }`

## Control Structure

Scratch hides most program structure behind event blocks such as `when green flag clicked` and `forever`. In Kotlin, the same ideas are visible in the source code.

| Kotlin structure | What it means | Scratch idea |
|------------------|---------------|--------------|
| `fun main(args: Array<String>)` | The normal entry point when the program is started by Gradle. | Similar to pressing the green flag. |
| `runBlocking { ... }` | Technical wrapper used by the project. Students normally leave it as it is. | Scratch hides this runtime detail. |
| `scratchStage(width, height, title) { ... }` | Opens the game window and runs the setup code inside the braces once. | Creates the stage and prepares sprites before the game starts. |
| `val player = rectangle(...) { ... }` | Creates one object and immediately configures it. | Creating a sprite and setting its first position, direction, or visibility. |
| `forever { ... }` | Registers code that runs once per rendered frame while the window is open. | Scratch's `forever` block. |
| `if (condition) { ... }` | Runs code only when a condition is true. | Scratch's `if then` block. |
| `when (value) { ... }` | Chooses between several named cases. | Similar to several `if` blocks, but cleaner for game states. |
| `while (condition) { ... }` | Repeats while a condition stays true. | Similar to Scratch's repeat blocks, but use it carefully in games. |
| `return@forever` | Stops the current frame update early and continues again next frame. | Similar to skipping the rest of one `forever` iteration. |

Code inside `scratchStage { ... }` but outside `forever { ... }` is setup code. It creates sprites, text labels, sounds, variables, and helper functions once.

Code inside `forever { ... }` is game-loop code. It should read input, move sprites, update text, check collisions, and react to game state. Do not put long blocking loops inside `forever`, because that would freeze the window instead of letting the next frame run.

In this repository, most exercise files use this shape:

```kotlin
fun main(args: Array<String>) {
    relaunchScratchMainWithModuleAccessIfNeeded(args)

    runBlocking {
        scratchStage(width = 800, height = 400, title = "My Stage") {
            val player = rectangle(width = 80, height = 40)

            forever {
                player.move(4)
            }
        }
    }
}
```

The important learner-facing parts are `scratchStage { ... }` for setup and `forever { ... }` for repeated game behavior. `relaunchScratchMainWithModuleAccessIfNeeded(args)` and `runBlocking { ... }` are technical wrapper code that makes desktop graphics work reliably from Gradle and the IDE. Students normally do not need to change them.

## Small Example

```kotlin
import de.moritzf.picoboard.scratch.ScratchRotationStyle
import de.moritzf.picoboard.scratch.internal.relaunchScratchMainWithModuleAccessIfNeeded
import de.moritzf.picoboard.scratch.scratchStage
import korlibs.event.Key
import korlibs.image.color.Colors
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    relaunchScratchMainWithModuleAccessIfNeeded(args)

    runBlocking {
        scratchStage(width = 1000, height = 700, title = "My First Stage") {
            val player = rectangle(
                width = 140.0,
                height = 24.0,
                color = Colors["#E2C044"],
            ) {
                goTo(0.0, -250.0)
                rotationStyle = ScratchRotationStyle.DONT_ROTATE
            }

            val ball = circle(
                radius = 16.0,
                color = Colors["#FF7F50"],
            ) {
                goTo(0.0, 40.0)
                pointInDirection(35.0)
            }

            val hitSound = sound("hit.wav")

            forever {
                if (keyPressed(Key.LEFT)) {
                    player.changeXBy(-6.0)
                }
                if (keyPressed(Key.RIGHT)) {
                    player.changeXBy(6.0)
                }

                ball.move(6.0)
                ball.ifOnEdgeBounce()

                if (ball.touching(player)) {
                    hitSound.play()
                    println("Hit")
                }
            }
        }
    }
}
```

Sound files belong in `src/main/resources/`, for example `hit.wav` or `hit.mp3`.

You can also play generated tones without providing a sound file:

```kotlin
playToneUntilDone("C", 0.5)
playToneUntilDone("C#", 0.5)
playToneUntilDone("H", 1.0)
```

Tone names use the German scale: `C`, `D`, `E`, `F`, `G`, `A`, `H`. `B` is accepted as an alias for `H`, and `Bb` plays B flat. Sharps and flats such as `C#` and `Cb` are supported, and you can add an octave number such as `C5`. Without an octave, octave 4 is used.

## Catch The Falling Ball Task

The student starter is here:

[CatchTheFallingBall.kt](../programming-exercise-tasks/src/main/kotlin/de/moritzf/picoboard/scratch/examples/catchthefallingball/CatchTheFallingBall.kt)

Run it from the repository root with:

```bash
./gradlew runCatchTheFallingBall
```

The full solution is here:

[CatchTheFallingBallSolution.kt](../solutions/src/main/kotlin/de/moritzf/picoboard/scratch/examples/catchthefallingball/solution/CatchTheFallingBallSolution.kt)

Run the full solution with:

```bash
./gradlew runCatchTheFallingBallSolution
```

You can also run the module tasks directly:

```bash
./gradlew :programming-exercise-tasks:runCatchTheFallingBall
./gradlew :solutions:runCatchTheFallingBallSolution
```

The solution first tries PicoBoard auto-selection. If no suitable device is found, it keeps running with keyboard controls.
