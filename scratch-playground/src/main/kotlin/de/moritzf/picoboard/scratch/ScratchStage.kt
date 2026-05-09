package de.moritzf.picoboard.scratch

import korlibs.audio.sound.SoundChannel
import korlibs.audio.sound.await
import korlibs.audio.sound.readSound
import korlibs.audio.sound.toSound
import korlibs.event.Key
import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.image.format.readBitmap
import korlibs.image.text.TextAlignment
import korlibs.io.file.std.resourcesVfs
import korlibs.korge.Korge
import korlibs.korge.KorgeDisplayMode
import korlibs.korge.view.*
import korlibs.math.geom.Size
import korlibs.render.GameWindowCreationConfig
import kotlinx.coroutines.*
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Opens a resizable game window and runs [init] to set up the stage.
 *
 * The coordinate system follows Scratch conventions: the origin (0, 0) is the center of the
 * stage, x increases to the right, and y increases upward.
 *
 * @param width logical width of the stage in pixels.
 * @param height logical height of the stage in pixels.
 * @param title window title shown in the title bar.
 * @param backgroundColor color rendered behind all sprites.
 * @param maxInitialWindowDimension the largest the initial window will be on either axis.
 *   The window is scaled down proportionally if the stage is larger than this value.
 * @param init suspend block that sets up sprites, loads assets, registers the game loop,
 *   and configures callbacks. The block runs in a coroutine context, so suspend functions
 *   such as [ScratchStage.image] can be called directly inside it.
 */
@Suppress("MagicNumber")
public suspend fun scratchStage(
    width: Int,
    height: Int,
    title: String = "Scratch Stage",
    backgroundColor: RGBA = Colors["#F5F1E8"],
    maxInitialWindowDimension: Int = 900,
    init: suspend ScratchStage.() -> Unit,
): Unit {
    require(width > 0) {
        "width must be greater than zero"
    }
    require(height > 0) {
        "height must be greater than zero"
    }
    require(maxInitialWindowDimension > 0) {
        "maxInitialWindowDimension must be greater than zero"
    }

    val initialWindow = fitIntoBoundingBox(width, height, maxInitialWindowDimension)
    configureMacOsRuntimeProperties(title)
    Korge(
        windowSize = Size(initialWindow.first, initialWindow.second),
        virtualSize = Size(width, height),
        displayMode = KorgeDisplayMode.CENTER,
        title = title,
        backgroundColor = surroundingStageColor(backgroundColor),
        windowCreationConfig = GameWindowCreationConfig(resizable = true),
    ).start {
        ScratchStage(this, CoroutineScope(coroutineContext), width, height, backgroundColor).init()
    }
}

private fun configureMacOsRuntimeProperties(applicationName: String): Unit {
    if (!System.getProperty("os.name").contains("Mac", ignoreCase = true)) {
        return
    }

    System.setProperty("apple.awt.application.name", applicationName)
    System.setProperty("apple.awt.application.appearance", "system")
    System.setProperty("sun.java2d.opengl", "false")
    System.setProperty("sun.java2d.metal", "true")
}

/**
 * A Scratch-style stage that hosts sprites and drives the game loop.
 *
 * Positions are expressed in Scratch coordinates: the origin (0, 0) is the center of the
 * stage, x increases to the right, and y increases upward. [width] and [height] are the full
 * logical dimensions, so the reachable x range is `[-width/2, width/2]` and the reachable y
 * range is `[-height/2, height/2]`.
 */
public class ScratchStage internal constructor(
    private val korgeStage: Stage,
    private val coroutineScope: CoroutineScope,
    /** Logical width of the stage in pixels. */
    public val width: Int,
    /** Logical height of the stage in pixels. */
    public val height: Int,
    backgroundColor: RGBA,
) {
    private val activeSoundChannels: MutableSet<SoundChannel> = mutableSetOf()
    private var soundSequence: Deferred<Unit> = CompletableDeferred(Unit)

    internal val stageHalfWidth: Double = width / 2.0
    internal val stageHalfHeight: Double = height / 2.0
    internal val stageCenterX: Double = width / 2.0
    internal val stageCenterY: Double = height / 2.0

    init {
        paintStageBackground(backgroundColor)
    }

    /**
     * Creates a filled rectangle sprite and adds it to the stage.
     *
     * The sprite is initially positioned at the stage center (0, 0). Use the [init] block or
     * the sprite's [ScratchSprite.goTo] method to move it after creation.
     *
     * @param width width of the rectangle in pixels.
     * @param height height of the rectangle in pixels.
     * @param color fill color.
     * @param init optional configuration block run immediately after the sprite is created.
     * @return the created [ScratchRectangleSprite].
     */
    @JvmOverloads
    public fun rectangle(
        width: Int,
        height: Int,
        color: RGBA = Colors.WHITE,
        init: ScratchRectangleSprite.() -> Unit = {},
    ): ScratchRectangleSprite {
        val sprite = ScratchRectangleSprite(
            stage = this,
            view = korgeStage.solidRect(width.toDouble(), height.toDouble(), color),
            width = width,
            height = height,
        )
        return sprite.apply(init)
    }

    /**
     * Creates a filled circle sprite and adds it to the stage.
     *
     * The sprite is initially positioned at the stage center (0, 0). Use the [init] block or
     * the sprite's [ScratchSprite.goTo] method to move it after creation.
     *
     * @param radius radius of the circle in pixels.
     * @param color fill color.
     * @param init optional configuration block run immediately after the sprite is created.
     * @return the created [ScratchCircleSprite].
     */
    @JvmOverloads
    public fun circle(
        radius: Int,
        color: RGBA = Colors.WHITE,
        init: ScratchCircleSprite.() -> Unit = {},
    ): ScratchCircleSprite {
        val sprite = ScratchCircleSprite(
            stage = this,
            view = korgeStage.circle(radius.toDouble(), fill = color, stroke = color),
            radius = radius,
        )
        return sprite.apply(init)
    }

    /**
     * Creates a text label and adds it to the stage.
     *
     * The label is initially positioned at the stage center (0, 0) and hidden. Use the [init]
     * block to move it and set its initial text before showing it.
     *
     * The [alignment] controls which point of the text bounding box the position refers to.
     * The default [TextAlignment.MIDDLE_CENTER] means x and y point to the center of the text,
     * consistent with how [circle] and [rectangle] sprites are positioned.
     *
     * @param text initial text content.
     * @param fontSize font size in pixels.
     * @param color text color.
     * @param alignment how the position relates to the text bounds.
     * @param init optional configuration block run immediately after the label is created.
     * @return the created [ScratchTextSprite].
     */
    @JvmOverloads
    public fun text(
        text: String = "",
        fontSize: Int = 32,
        color: RGBA = Colors.WHITE,
        alignment: TextAlignment = TextAlignment.MIDDLE_CENTER,
        init: ScratchTextSprite.() -> Unit = {},
    ): ScratchTextSprite {
        val view = korgeStage.text(text, textSize = fontSize.toDouble(), color = color, alignment = alignment)
        val sprite = ScratchTextSprite(this, view)
        return sprite.apply(init)
    }

    /**
     * Loads a PNG (or other image format) from the resources folder, creates an image sprite,
     * and adds it to the stage.
     *
     * Place image files in `src/main/resources/` and refer to them by filename, e.g.
     * `"player.png"`. Transparency in the image is rendered correctly. Collision detection
     * uses a rectangular bounding box that defaults to the image's natural pixel dimensions;
     * adjust [ScratchImageSprite.collisionWidth] and [ScratchImageSprite.collisionHeight] in
     * the [init] block to exclude transparent padding.
     *
     * This is a suspend function and must be called from within the [scratchStage] init block
     * or another suspend context.
     *
     * @param path resource-relative path to the image file, e.g. `"player.png"`.
     * @param init optional configuration block run immediately after the sprite is created.
     * @return the created [ScratchImageSprite].
     */
    public suspend fun image(
        path: String,
        init: ScratchImageSprite.() -> Unit = {},
    ): ScratchImageSprite {
        val bitmap = resourcesVfs[path].readBitmap().toBMP32()
        val view = korgeStage.image(bitmap)
        val sprite = ScratchImageSprite(
            stage = this,
            view = view,
            bitmap = bitmap,
            imageWidth = bitmap.width.toDouble(),
            imageHeight = bitmap.height.toDouble(),
        )
        return sprite.apply(init)
    }

    /**
     * Loads a sound file from the resources folder.
     *
     * Place sound files in `src/main/resources/` and refer to them by filename, e.g.
     * `"jump.wav"`. WAV and MP3 files are supported by KorGE on the JVM.
     *
     * This is a suspend function and must be called from within the [scratchStage] init block
     * or another suspend context.
     *
     * @param path resource-relative path to the sound file, e.g. `"jump.wav"`.
     * @return the loaded [ScratchSound].
     */
    public suspend fun sound(path: String): ScratchSound {
        return ScratchSound(resourcesVfs[path].readSound(), ::registerSoundChannel)
    }

    /**
     * Generates and starts playing a tone without needing a sound file.
     *
     * Notes use German names: `C`, `D`, `E`, `F`, `G`, `A`, `H`, plus sharps and flats such as
     * `C#` and `Cb`. `B` is accepted as an alias for `H`; use `Bb` for B flat. Add an octave number when needed,
     * for example `C5`; without an octave, octave 4 is used.
     *
     * @param note note name, e.g. `C`, `C#`, `Cb`, `H`, or `C5`.
     * @param durationSeconds playback duration in seconds.
     * @param volume playback volume from `0.0` to `1.0`.
     */
    public fun playTone(
        note: String,
        durationSeconds: Double,
        volume: Double = 0.5,
    ): Unit {
        validateTone(durationSeconds, volume)
        coroutineScope.launch {
            playToneAndWait(note, durationSeconds, volume)
        }
    }

    /**
     * Queues a generated tone after previously queued tones.
     *
     * This function intentionally is not `suspend`, so beginner exercises can write a melody as
     * a simple sequence of statements.
     */
    public fun playToneUntilDone(
        note: String,
        durationSeconds: Double,
        volume: Double = 0.5,
    ): Unit {
        validateTone(durationSeconds, volume)
        val previousSoundSequence = soundSequence
        soundSequence = coroutineScope.async {
            previousSoundSequence.await()
            playToneAndWait(note, durationSeconds, volume)
        }
    }

    /**
     * Registers a block that is called once per frame for the lifetime of the stage.
     *
     * This is the main game loop entry point. Read sensor values, update sprite positions, and
     * check game conditions inside [block]. The block runs on the render thread; avoid blocking
     * calls.
     *
     * @param block code to execute every frame, with this stage as receiver.
     */
    public fun forever(block: ScratchStage.() -> Unit): Unit {
        korgeStage.addUpdater {
            block()
        }
    }

    /**
     * Returns `true` if [key] is currently held down.
     *
     * @param key the key to check.
     */
    public fun keyPressed(key: Key): Boolean {
        return korgeStage.views.input.keys[key]
    }

    /**
     * Returns `true` only on the frame where [key] changes from not pressed to pressed.
     */
    public fun keyJustPressed(key: Key): Boolean {
        return korgeStage.views.input.keys.justPressed(key)
    }

    /**
     * Stops all sounds started through [ScratchSound.play], [ScratchSound.playUntilDone], or
     * [ScratchSound.loop].
     */
    public fun stopAllSounds(): Unit {
        soundSequence.cancel()
        soundSequence = CompletableDeferred(Unit)
        for (channel in activeSoundChannels) {
            channel.stop()
        }
        activeSoundChannels.clear()
    }

    /**
     * Registers a suspend [block] that is called when the game window is closed.
     *
     * Use this to release resources such as hardware connections.
     *
     * @param block suspend function to call on window close.
     */
    public fun onClose(block: suspend () -> Unit): Unit {
        korgeStage.views.onClose(block)
    }

    private fun registerSoundChannel(channel: SoundChannel): Unit {
        activeSoundChannels.add(channel)
    }

    private fun paintStageBackground(backgroundColor: RGBA): Unit {
        korgeStage.solidRect(width.toDouble(), height.toDouble(), backgroundColor)
    }

    private suspend fun playToneAndWait(
        note: String,
        durationSeconds: Double,
        volume: Double,
    ): Unit {
        val sound = ScratchTone.generate(note, durationSeconds, volume).toSound()
        sound.playNoCancel().also(::registerSoundChannel).await()
    }

    private fun validateTone(durationSeconds: Double, volume: Double): Unit {
        require(durationSeconds > 0.0) {
            "durationSeconds must be greater than zero"
        }
        require(volume in 0.0..1.0) {
            "volume must be between 0.0 and 1.0"
        }
    }
}

private fun fitIntoBoundingBox(
    width: Int,
    height: Int,
    maxDimension: Int,
): Pair<Int, Int> {
    val scale = minOf(maxDimension / width.toDouble(), maxDimension / height.toDouble(), 1.0)
    return max(320, (width * scale).roundToInt()) to max(240, (height * scale).roundToInt())
}

private const val SURROUNDING_STAGE_ADJUSTMENT = 0.12
private const val DARK_STAGE_LUMINANCE_THRESHOLD = 128.0

private fun surroundingStageColor(backgroundColor: RGBA): RGBA {
    val luminance = 0.299 * backgroundColor.r + 0.587 * backgroundColor.g + 0.114 * backgroundColor.b
    val target = if (luminance < DARK_STAGE_LUMINANCE_THRESHOLD) 255 else 0
    return RGBA(
        mixColorComponent(backgroundColor.r, target),
        mixColorComponent(backgroundColor.g, target),
        mixColorComponent(backgroundColor.b, target),
        backgroundColor.a,
    )
}

private fun mixColorComponent(component: Int, target: Int): Int {
    return (component + (target - component) * SURROUNDING_STAGE_ADJUSTMENT).roundToInt()
}
