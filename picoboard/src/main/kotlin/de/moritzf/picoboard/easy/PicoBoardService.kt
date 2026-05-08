package de.moritzf.picoboard.easy

import de.moritzf.picoboard.PicoBoardException
import de.moritzf.picoboard.PicoBoardPollingHandle
import de.moritzf.picoboard.PicoBoardScaledValues

/**
 * A connected PicoBoard session that polls in the background and always exposes the latest
 * sensor values.
 *
 * On construction an initial frame is read synchronously, so sensor values are available
 * immediately after [PicoBoardEasy.startService] returns. A background thread then continues
 * refreshing values at the configured interval.
 *
 * All sensor accessors ([slider], [light], [sound], etc.) return the value from the most
 * recently received frame and never block.
 *
 * If the background polling thread encounters too many consecutive read failures it stops and
 * records the cause. Check [isRunning] and [failure] to detect this situation, or call
 * [throwIfFailed] to propagate it as an exception.
 *
 * Instances must be closed when no longer needed, either explicitly via [close] or with a
 * `use` block. Closing stops the background polling thread and releases the serial port.
 */
public class PicoBoardService internal constructor(
    private val project: PicoBoardProject,
    intervalMillis: Long,
) : AutoCloseable {

    @Volatile
    private var cachedValues: PicoBoardScaledValues

    private val pollingHandle: PicoBoardPollingHandle

    init {
        cachedValues = project.update().values()
        pollingHandle = project.startPolling(intervalMillis) {
            cachedValues = values()
        }
    }

    /**
     * The identifier of the serial port this service is connected to.
     */
    public val portIdentifier: String
        get() = project.portIdentifier

    /**
     * Returns all scaled sensor values from the most recently received frame.
     */
    public fun values(): PicoBoardScaledValues = cachedValues

    /**
     * Returns the current slider position as a value from 0 (left) to 100 (right).
     */
    public fun slider(): Int = cachedValues.slider

    /**
     * Returns the current sound level as a value from 0 (quiet) to 100 (loud).
     */
    public fun sound(): Int = cachedValues.sound

    /**
     * Returns the current light level as a value from 0 (dark) to 100 (bright).
     */
    public fun light(): Int = cachedValues.light

    /**
     * Returns `true` if the PicoBoard button is currently pressed.
     */
    public fun buttonPressed(): Boolean = cachedValues.buttonPressed

    /**
     * Returns the current resistance on connector A as a value from 0 to 100.
     */
    public fun resistanceA(): Int = cachedValues.resistanceA

    /**
     * Returns the current resistance on connector B as a value from 0 to 100.
     */
    public fun resistanceB(): Int = cachedValues.resistanceB

    /**
     * Returns the current resistance on connector C as a value from 0 to 100.
     */
    public fun resistanceC(): Int = cachedValues.resistanceC

    /**
     * Returns the current resistance on connector D as a value from 0 to 100.
     */
    public fun resistanceD(): Int = cachedValues.resistanceD

    /**
     * Returns `true` if the background polling thread is still running.
     *
     * Returns `false` if polling was stopped via [close] or if it stopped due to repeated
     * read failures. In the latter case, [failure] returns the cause.
     */
    public fun isRunning(): Boolean = pollingHandle.isRunning()

    /**
     * Returns the exception that caused polling to stop, or `null` if polling is still
     * running or was stopped normally via [close].
     */
    public fun failure(): Throwable? = pollingHandle.failure()

    /**
     * Throws an [IllegalStateException] wrapping the polling failure if polling has stopped
     * due to an error. Does nothing if polling is still running or was stopped normally.
     *
     * @throws IllegalStateException if background polling failed.
     */
    @Throws(PicoBoardException::class)
    public fun throwIfFailed(): Unit = pollingHandle.throwIfFailed()

    /**
     * Stops background polling and closes the connection to the PicoBoard.
     *
     * Calling [close] more than once is safe.
     */
    public override fun close() {
        pollingHandle.stop()
        project.close()
    }
}
