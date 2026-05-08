# picoboard

`picoboard` is a Kotlin/JVM library for reading a Scratch-era PicoBoard from Kotlin and Java applications over a serial connection.

The library:

- uses a Gradle build with a standard wrapper
- works on macOS, Linux, and Windows through `jSerialComm`
- exposes both raw 10-bit sensor values and Scratch-style scaled values
- supports one-shot reads and scheduled polling
- uses a Java 21 toolchain for building and running the included Gradle tasks

## Status

The implementation targets the classic PicoBoard serial protocol used by Scratch 1.x:

- `38400` baud, `8N1`
- host sends poll byte `0x01`
- board replies with an 18-byte packet made of nine high/low channel pairs

The parser and scaling logic follow the MIT Scratch Board technical guide, and the serial setup matches the SparkFun PicoBoard getting-started documentation.

## Build

```bash
./gradlew build
```

Gradle is configured to use Java 21 toolchains across the repository.
If Java 21 is not installed locally, Gradle can provision it automatically.

## IntelliJ on Linux

On this project, a normal IntelliJ installation on Linux works with the PicoBoard.
Testing with IntelliJ's bundled JetBrains Runtime successfully opened `/dev/ttyUSB0`
and read a valid PicoBoard packet.

If PicoBoard access fails from your IDE, the usual causes are outside the library:

- the IDE was installed from a sandboxed package format that restricts device access
- the run configuration is using a different runtime or environment than your shell
- the wrong serial device was selected

For Linux development, prefer a normal JetBrains Toolbox or tarball installation
over sandboxed package formats when you need direct access to `/dev/ttyUSB*`.

The runnable Kotlin starter example is in:

[Main.kt](programming-exercise-tasks/src/main/kotlin/de/moritzf/picoboard/examples/firstproject/Main.kt)

Run it with:

```bash
./gradlew readSensorValues
```

## Projects

This repository is split into four Gradle projects:

- `:picoboard` contains the PicoBoard library and CLI
- `:scratch-playground` contains the KorGE-based Scratch-style API
- `:programming-exercise-tasks` contains starter tasks for students
- `:solutions` contains completed solutions

## Scratch Playground

The repository also contains an optional KorGE-based playground module with a Scratch-shaped API for simple 2D projects:

- fixed logical stage size with a resizable window that scales automatically
- simple `rectangle(...)` and `circle(...)` sprites
- centered Scratch-like coordinates
- sprite properties such as `x`, `y`, `direction`, `size`, `scale`, `rotationStyle`, and `visible`
- helpers such as `move(...)`, `turnLeft(...)`, `turnRight(...)`, `touching(...)`, `touchingEdge()`, and `ifOnEdgeBounce()`

The playground guide is in:

[scratch-playground/README.md](scratch-playground/README.md)

The included catch-the-falling-ball starter is in:

[CatchTheFallingBall.kt](programming-exercise-tasks/src/main/kotlin/de/moritzf/picoboard/scratch/examples/catchthefallingball/CatchTheFallingBall.kt)

Run it with:

```bash
./gradlew runCatchTheFallingBall
```

The full solution is in:

[CatchTheFallingBallSolution.kt](solutions/src/main/kotlin/de/moritzf/picoboard/scratch/examples/catchthefallingball/solution/CatchTheFallingBallSolution.kt)

Run it with:

```bash
./gradlew runCatchTheFallingBallSolution
```

The solution tries PicoBoard auto-selection first. If no suitable board is available, it falls back to keyboard controls.

## CLI Sample

List available serial ports:

```bash
./gradlew :picoboard:run --args="--list-ports"
```

Read continuously with library auto-selection:

```bash
./gradlew :picoboard:run --args="--interval-ms 100"
```

Read continuously from a specific PicoBoard:

```bash
./gradlew :picoboard:run --args="--port /dev/cu.usbserial-A5061E1Q --interval-ms 100"
```

Read 10 frames and exit:

```bash
./gradlew :picoboard:run --args="--port /dev/cu.usbserial-A5061E1Q --count 10"
```

Install the CLI with startup scripts:

```bash
./gradlew :picoboard:installDist
picoboard/build/install/picoboard/bin/picoboard --port /dev/cu.usbserial-A5061E1Q --count 10
```

## Auto-Selection

The library can auto-select a suitable serial device:

- `PicoBoard.findAutoSelectedPort()` returns the uniquely best match or `null`
- `PicoBoard.requireAutoSelectedPort()` throws if no suitable device is found or if selection is ambiguous
- `PicoBoard.open()` auto-selects a suitable device and opens it

If you want full control, keep using `PicoBoard.open(portPath)` or `PicoBoard.open(port)`.

## Kotlin Example

```kotlin
import de.moritzf.picoboard.PicoBoard

fun main() {
    PicoBoard.open().use { board ->
        val frame = board.readFrame()

        println("Light: ${frame.scaled.light}")
        println("Sound: ${frame.scaled.sound}")
        println("Button pressed: ${frame.scaled.buttonPressed}")
        println("Resistance A raw: ${frame.raw.resistanceA}")
    }
}
```

## Java Example

```java
import de.moritzf.picoboard.PicoBoard;
import de.moritzf.picoboard.PicoBoardFrame;

public final class ReadPicoBoard {
    public static void main(String[] args) throws Exception {
        try (var board = PicoBoard.open()) {
            PicoBoardFrame frame = board.readFrame();
            System.out.println("Slider: " + frame.getScaled().getSlider());
            System.out.println("Button pressed: " + frame.getScaled().isButtonPressed());
        }
    }
}
```

## Notes

- On older systems, you may need FTDI VCP drivers installed before the PicoBoard appears as a serial device.
- On macOS, the relevant port is typically `/dev/tty.usbserial-*` or `/dev/cu.usbserial-*`.
- On Linux, it is commonly `/dev/ttyUSB*`.
- On Linux, prefer the stable symlink under `/dev/serial/by-id/` when possible, for example `/dev/serial/by-id/usb-FTDI_FT232R_USB_UART_A5061E1Q-if00-port0`.
- On Linux with IntelliJ, a Toolbox-installed IDE should be able to access the PicoBoard directly. If the IDE was installed through a sandboxed package format, serial device access may be blocked by the package runtime instead of by this library.
- On Windows, it appears as `COMx`.
