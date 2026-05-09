package de.moritzf.picoboard.scratch

import de.moritzf.picoboard.scratch.internal.ScratchBounds
import de.moritzf.picoboard.scratch.internal.ScratchCircleShape
import de.moritzf.picoboard.scratch.internal.ScratchImageShape
import de.moritzf.picoboard.scratch.internal.ScratchRectangleShape
import de.moritzf.picoboard.scratch.internal.ScratchShape
import de.moritzf.picoboard.scratch.internal.ScratchVector
import de.moritzf.picoboard.scratch.internal.bounceDirection
import de.moritzf.picoboard.scratch.internal.clampPositionInsideStage
import de.moritzf.picoboard.scratch.internal.directionVector
import de.moritzf.picoboard.scratch.internal.displayRotationDegrees
import de.moritzf.picoboard.scratch.internal.movePoint
import de.moritzf.picoboard.scratch.internal.normalizeDirection
import de.moritzf.picoboard.scratch.internal.spriteRotationDegrees
import de.moritzf.picoboard.scratch.internal.touching
import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.korge.view.Anchorable
import korlibs.korge.view.Image
import korlibs.korge.view.Text
import korlibs.korge.view.View
import korlibs.korge.view.centered
import korlibs.math.geom.degrees
import kotlin.math.roundToInt

/**
 * Base class for all visible objects on a [ScratchStage].
 *
 * Position, direction, size, and rotation style are all expressed in Scratch conventions:
 *
 * - **x / y** — stage coordinates with the origin at the center; x increases right, y increases up.
 * - **direction** — degrees clockwise from straight up (north). 90° points right, 180° points
 *   down, -90° (or 270°) points left. The value is always normalised to the range (-180, 180].
 * - **size** — percentage of the original size; 100 means no scaling.
 */
public sealed class ScratchSprite protected constructor(
    internal val stage: ScratchStage,
    protected val view: View,
) {
    private var removed: Boolean = false

    /**
     * Horizontal position in stage coordinates. 0 is the center; negative values are to the
     * left, positive to the right.
     */
    public var x: Int = 0
        set(value) {
            field = value
            syncView()
        }

    /**
     * Vertical position in stage coordinates. 0 is the center; negative values are below,
     * positive above.
     */
    public var y: Int = 0
        set(value) {
            field = value
            syncView()
        }

    /**
     * Direction the sprite is facing, in degrees clockwise from straight up (north).
     *
     * The value is automatically normalised to the range (-180, 180] on every assignment.
     */
    public var direction: Double = 90.0
        set(value) {
            field = normalizeDirection(value)
            syncView()
        }

    /**
     * Visual size of the sprite as a percentage of its original size. Must be greater than zero.
     *
     * 100 means the sprite is rendered at its original dimensions. Setting this to 200 doubles
     * it, and 50 halves it.
     */
    public var size: Double = 100.0
        set(value) {
            require(value > 0.0) {
                "size must be greater than zero"
            }
            field = value
            syncView()
        }

    /**
     * Controls how the sprite image rotates when [direction] changes.
     *
     * @see ScratchRotationStyle
     */
    public var rotationStyle: ScratchRotationStyle = ScratchRotationStyle.ALL_AROUND
        set(value) {
            field = value
            syncView()
        }

    /**
     * Whether the sprite is rendered on screen. `true` means visible, `false` means hidden.
     */
    public var visible: Boolean
        get() = view.visible
        set(value) {
            view.visible = value
        }

    /**
     * Visual scale factor of the sprite. This is a direct mapping of [size] to a 0–1 fraction:
     * a [size] of 100 gives a [scale] of 1.0.
     *
     * Setting [scale] updates [size] and vice versa.
     */
    public var scale: Double
        get() = size / 100.0
        set(value) {
            size = value * 100.0
        }

    /** Fill color of the sprite. */
    public abstract var color: RGBA

    init {
        if (view is Anchorable) {
            view.centered
        }
        syncView()
    }

    /**
     * Makes the sprite visible.
     *
     * Equivalent to setting `visible = true`.
     */
    public fun show() {
        visible = true
    }

    /**
     * Hides the sprite.
     *
     * Equivalent to setting `visible = false`.
     */
    public fun hide() {
        visible = false
    }

    /**
     * Moves the sprite to the given stage coordinates.
     *
     * @param x target horizontal position.
     * @param y target vertical position.
     */
    public fun goTo(x: Int, y: Int) {
        this.x = x
        this.y = y
    }

    /**
     * Shifts the sprite horizontally by [dx] pixels.
     *
     * @param dx number of pixels to move right (negative moves left).
     */
    public fun changeXBy(dx: Int) {
        x += dx
    }

    /**
     * Shifts the sprite vertically by [dy] pixels.
     *
     * @param dy number of pixels to move up (negative moves down).
     */
    public fun changeYBy(dy: Int) {
        y += dy
    }

    /**
     * Sets the sprite's [direction] to [directionDegrees].
     *
     * This is a named alias for assigning [direction] directly, matching the Scratch block name.
     *
     * @param directionDegrees direction in degrees clockwise from north.
     */
    public fun pointInDirection(directionDegrees: Int) {
        direction = directionDegrees.toDouble()
    }

    /**
     * Rotates the sprite to face [other].
     *
     * If both sprites occupy the same position the direction is not changed.
     *
     * @param other the sprite to point towards.
     */
    public fun pointTowards(other: ScratchSprite) {
        val deltaX = other.x - x
        val deltaY = other.y - y
        if (deltaX == 0 && deltaY == 0) {
            return
        }
        direction = Math.toDegrees(kotlin.math.atan2(deltaX.toDouble(), deltaY.toDouble()))
    }

    /**
     * Moves the sprite [steps] pixels in its current [direction].
     *
     * @param steps number of pixels to move forward (negative moves backward).
     */
    public fun move(steps: Int) {
        val moved = movePoint(ScratchVector(x.toDouble(), y.toDouble()), steps.toDouble(), direction)
        this.x = moved.x.roundToInt()
        this.y = moved.y.roundToInt()
    }

    /**
     * Rotates the sprite [degrees] clockwise.
     *
     * @param degrees angle to rotate right.
     */
    public fun turnRight(degrees: Int) {
        direction += degrees
    }

    /**
     * Rotates the sprite [degrees] counter-clockwise.
     *
     * @param degrees angle to rotate left.
     */
    public fun turnLeft(degrees: Int) {
        direction -= degrees
    }

    /**
     * Returns `true` if this sprite's bounding shape overlaps [other]'s bounding shape.
     *
     * Both sprites must still be active (not [remove]d).
     *
     * @param other the sprite to test collision against.
     */
    public fun touching(other: ScratchSprite): Boolean {
        ensureActive()
        other.ensureActive()
        return touching(currentShape(), other.currentShape())
    }

    /**
     * Returns `true` if any part of the sprite lies outside the stage boundaries.
     */
    public fun touchingEdge(): Boolean {
        ensureActive()
        return currentBounds().touchesStage(stage.stageHalfWidth, stage.stageHalfHeight).hasCollision
    }

    /**
     * Reflects the sprite's direction if it is touching a stage edge, and clamps its position
     * back inside the stage.
     *
     * Equivalent to the Scratch "if on edge, bounce" block.
     */
    public fun ifOnEdgeBounce() {
        ensureActive()
        val bounds = currentBounds()
        val edgeCollision = bounds.touchesStage(stage.stageHalfWidth, stage.stageHalfHeight)
        if (!edgeCollision.hasCollision) {
            return
        }

        val clamped = clampPositionInsideStage(
            center = ScratchVector(x.toDouble(), y.toDouble()),
            bounds = bounds,
            stageHalfWidth = stage.stageHalfWidth,
            stageHalfHeight = stage.stageHalfHeight,
        )
        x = clamped.x.roundToInt()
        y = clamped.y.roundToInt()
        direction = bounceDirection(direction, edgeCollision)
    }

    /**
     * Removes the sprite from the stage permanently.
     *
     * After calling [remove], the sprite can no longer be used in collision checks or moved.
     * Calling [remove] more than once is safe.
     */
    public fun remove() {
        if (removed) {
            return
        }
        removed = true
        view.removeFromParent()
    }

    internal fun currentBounds(): ScratchBounds = currentShape().axisAlignedBounds()

    internal abstract fun currentShape(): ScratchShape

    private fun syncView() {
        if (removed) {
            return
        }
        view.x = stage.stageCenterX + x
        view.y = stage.stageCenterY - y
        val visualScale = size / 100.0
        when (rotationStyle) {
            ScratchRotationStyle.ALL_AROUND -> {
                view.scaleX = visualScale
                view.scaleY = visualScale
                view.rotation = displayRotationDegrees(direction, rotationStyle).degrees
            }
            ScratchRotationStyle.LEFT_RIGHT -> {
                val facingRight = directionVector(direction).x >= 0.0
                view.scaleX = if (facingRight) visualScale else -visualScale
                view.scaleY = visualScale
                view.rotation = 0.degrees
            }
            ScratchRotationStyle.DONT_ROTATE -> {
                view.scaleX = visualScale
                view.scaleY = visualScale
                view.rotation = 0.degrees
            }
        }
    }

    private fun ensureActive() {
        check(!removed) {
            "Scratch sprite has already been removed"
        }
    }

    protected fun shapeRotationDegrees(): Double {
        return spriteRotationDegrees(direction, rotationStyle)
    }
}

/**
 * A filled circle sprite created via [ScratchStage.circle].
 *
 * The circle is centered on its position. Its effective collision radius scales with [scale].
 *
 * @property radius base radius in pixels, before any scaling.
 */
public class ScratchCircleSprite internal constructor(
    stage: ScratchStage,
    view: korlibs.korge.view.Circle,
    /** Base radius in pixels, before any [scale] is applied. */
    public val radius: Int,
) : ScratchSprite(stage, view) {
    private val circleView: korlibs.korge.view.Circle = view

    override var color: RGBA
        get() = circleView.color
        set(value) {
            circleView.color = value
        }

    override fun currentShape(): ScratchShape {
        return ScratchCircleShape(
            center = ScratchVector(x.toDouble(), y.toDouble()),
            radius = radius * scale,
        )
    }
}

/**
 * A filled rectangle sprite created via [ScratchStage.rectangle].
 *
 * The rectangle is centered on its position. Its effective dimensions scale with [scale].
 *
 * @property width base width in pixels, before any scaling.
 * @property height base height in pixels, before any scaling.
 */
public class ScratchRectangleSprite internal constructor(
    stage: ScratchStage,
    view: korlibs.korge.view.SolidRect,
    /** Base width in pixels, before any [scale] is applied. */
    public val width: Int,
    /** Base height in pixels, before any [scale] is applied. */
    public val height: Int,
) : ScratchSprite(stage, view) {
    private val rectangleView: korlibs.korge.view.SolidRect = view

    override var color: RGBA
        get() = rectangleView.color
        set(value) {
            rectangleView.color = value
        }

    override fun currentShape(): ScratchShape {
        return ScratchRectangleShape(
            center = ScratchVector(x.toDouble(), y.toDouble()),
            halfWidth = (width * scale) / 2.0,
            halfHeight = (height * scale) / 2.0,
            rotationDegrees = shapeRotationDegrees(),
        )
    }
}

/**
 * An image sprite created via [ScratchStage.image].
 *
 * The image is rendered with full transparency support. Collision detection is pixel-perfect:
 * two sprites are considered touching only when a non-transparent pixel of one overlaps a
 * non-transparent pixel (or the solid area) of the other. Fully transparent pixels never
 * participate in collisions.
 *
 * The sprite is centered on its [x]/[y] position. Its visual size and collision shape both
 * scale with [ScratchSprite.scale].
 *
 * Tint the image with [color]; [Colors.WHITE] (the default) leaves colors unchanged.
 */
public class ScratchImageSprite internal constructor(
    stage: ScratchStage,
    view: Image,
    private val bitmap: Bitmap32,
    private val imageWidth: Double,
    private val imageHeight: Double,
) : ScratchSprite(stage, view) {
    private val imageView: Image = view

    /**
     * Tint color multiplied with every pixel of the image. [Colors.WHITE] (the default)
     * leaves the image colors unchanged. Any other color tints the image toward that hue.
     */
    override var color: RGBA
        get() = imageView.colorMul
        set(value) {
            imageView.colorMul = value
        }

    override fun currentShape(): ScratchShape {
        return ScratchImageShape(
            center = ScratchVector(x.toDouble(), y.toDouble()),
            bitmap = bitmap,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            scale = scale,
            rotationDegrees = shapeRotationDegrees(),
            facingLeft = rotationStyle == ScratchRotationStyle.LEFT_RIGHT &&
                directionVector(direction).x < 0,
        )
    }
}

/**
 * A text label created via [ScratchStage.text].
 *
 * Unlike shape sprites, a text label has no collision detection. Its position refers to a
 * configurable anchor point on the text bounding box, controlled by the [alignment] passed
 * to [ScratchStage.text] (default: center).
 */
public class ScratchTextSprite internal constructor(
    private val stage: ScratchStage,
    private val view: Text,
) {
    /**
     * Horizontal position in stage coordinates. 0 is the center; negative values are to the
     * left, positive to the right.
     */
    public var x: Int = 0
        set(value) {
            field = value
            syncView()
        }

    /**
     * Vertical position in stage coordinates. 0 is the center; negative values are below,
     * positive above.
     */
    public var y: Int = 0
        set(value) {
            field = value
            syncView()
        }

    /** The text string currently displayed. */
    public var text: String
        get() = view.text
        set(value) {
            view.text = value
        }

    /** Color of the rendered text. */
    public var color: RGBA
        get() = view.color
        set(value) {
            view.color = value
        }

    /** Font size in pixels. */
    public var fontSize: Int
        get() = view.textSize.toInt()
        set(value) {
            view.textSize = value.toDouble()
        }

    /** Whether the label is rendered on screen. `true` means visible, `false` means hidden. */
    public var visible: Boolean
        get() = view.visible
        set(value) {
            view.visible = value
        }

    init {
        syncView()
    }

    /**
     * Makes the label visible.
     *
     * Equivalent to setting `visible = true`.
     */
    public fun show() {
        visible = true
    }

    /**
     * Hides the label.
     *
     * Equivalent to setting `visible = false`.
     */
    public fun hide() {
        visible = false
    }

    /**
     * Moves the label to the given stage coordinates.
     *
     * @param x target horizontal position.
     * @param y target vertical position.
     */
    public fun goTo(x: Int, y: Int) {
        this.x = x
        this.y = y
    }

    private fun syncView() {
        view.x = stage.stageCenterX + x
        view.y = stage.stageCenterY - y
    }
}
