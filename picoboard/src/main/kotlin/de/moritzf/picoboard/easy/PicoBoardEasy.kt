package de.moritzf.picoboard.easy

import de.moritzf.picoboard.PicoBoard
import de.moritzf.picoboard.PicoBoardConnection
import de.moritzf.picoboard.PicoBoardException
import de.moritzf.picoboard.PicoBoardFrame
import de.moritzf.picoboard.PicoBoardListener
import de.moritzf.picoboard.PicoBoardOptions
import de.moritzf.picoboard.PicoBoardPollingHandle
import de.moritzf.picoboard.PicoBoardScaledValues
import java.time.Duration

/**
 * Entry point for the easy PicoBoard API.
 *
 * Provides three ways to connect to a PicoBoard, in increasing order of complexity:
 *
 * - [run] — opens a connection, executes a block, and closes the connection automatically.
 * - [connect] — opens a connection and returns a [PicoBoardProject] for manual control.
 * - [startService] — opens a connection, starts background polling, and returns a
 *   [PicoBoardService] whose sensor values are always up to date.
 *
 * When no [portPath] is given, the port is selected automatically.
 */
public object PicoBoardEasy {

    /**
     * Opens a connection to the PicoBoard, runs [block], and closes the connection when done.
     *
     * The [PicoBoardProject] receiver inside [block] provides sensor accessors and loop helpers.
     * The connection is closed even if [block] throws.
     *
     * @param portPath serial port to connect to, or `null` to auto-detect.
     * @param options connection and polling configuration.
     * @param block code to run with the connected board.
     * @throws PicoBoardException if the connection cannot be opened or a read fails.
     */
    @JvmStatic
    @JvmOverloads
    @Throws(PicoBoardException::class)
    public fun run(
        portPath: String? = null,
        options: PicoBoardOptions = PicoBoardOptions(),
        block: PicoBoardProject.() -> Unit,
    ): Unit {
        openConnection(portPath, options).use { connection ->
            PicoBoardProject(connection).block()
        }
    }

    /**
     * Opens a connection to the PicoBoard and returns a [PicoBoardProject].
     *
     * The caller is responsible for closing the project when done, for example with
     * [PicoBoardProject.close] or a `use` block.
     *
     * @param portPath serial port to connect to, or `null` to auto-detect.
     * @param options connection and polling configuration.
     * @return a connected [PicoBoardProject].
     * @throws PicoBoardException if the connection cannot be opened.
     */
    @JvmStatic
    @JvmOverloads
    @Throws(PicoBoardException::class)
    public fun connect(
        portPath: String? = null,
        options: PicoBoardOptions = PicoBoardOptions(),
    ): PicoBoardProject {
        return PicoBoardProject(openConnection(portPath, options))
    }

    /**
     * Opens a connection to the PicoBoard, performs an initial read, starts background polling,
     * and returns a [PicoBoardService].
     *
     * Sensor values on the returned service are refreshed automatically in the background at
     * [intervalMillis] and can be read at any time without blocking. The caller is responsible
     * for closing the service when done.
     *
     * @param portPath serial port to connect to, or `null` to auto-detect.
     * @param options connection and polling configuration.
     * @param intervalMillis how often to poll the board, in milliseconds.
     *   Defaults to [PicoBoardOptions.pollingInterval].
     * @return a running [PicoBoardService] with an initial frame already available.
     * @throws PicoBoardException if the connection cannot be opened or the initial read fails.
     */
    @JvmStatic
    @JvmOverloads
    @Throws(PicoBoardException::class)
    public fun startService(
        portPath: String? = null,
        options: PicoBoardOptions = PicoBoardOptions(),
        intervalMillis: Long = options.pollingInterval.toMillis(),
    ): PicoBoardService {
        val project = connect(portPath, options)
        return PicoBoardService(project, intervalMillis)
    }

    private fun openConnection(
        portPath: String?,
        options: PicoBoardOptions,
    ): PicoBoardConnection {
        return if (portPath == null) {
            PicoBoard.open(options)
        } else {
            PicoBoard.open(portPath, options)
        }
    }
}

/**
 * A connected PicoBoard session for reading sensor values manually or in loops.
 *
 * Sensor values are not read automatically — call [update] to fetch a fresh frame from the
 * board, or use [every] / [repeat] which call it for you on each iteration. Once a frame has
 * been fetched, all sensor accessors ([slider], [light], [sound], etc.) return values from
 * the most recently fetched frame without performing another read.
 *
 * For a simpler API that polls in the background and always exposes the latest values, see
 * [PicoBoardService] via [PicoBoardEasy.startService].
 *
 * Instances must be closed when no longer needed, either explicitly via [close] or with a
 * `use` block.
 */
public class PicoBoardProject internal constructor(
    private val connection: PicoBoardConnection,
    private val sleeper: (Long) -> Unit = { Thread.sleep(it) },
) : AutoCloseable {
    @Volatile
    private var lastFrame: PicoBoardFrame? = null

    /**
     * The identifier of the serial port this project is connected to.
     */
    public val portIdentifier: String
        get() = connection.portIdentifier

    /**
     * Reads a fresh frame from the PicoBoard and caches it.
     *
     * Subsequent calls to sensor accessors ([slider], [light], etc.) return values from this
     * cached frame until the next [update] call.
     *
     * @return this instance, for chaining.
     * @throws PicoBoardException if the read fails.
     */
    @Throws(PicoBoardException::class)
    public fun update(): PicoBoardProject {
        lastFrame = connection.readFrame()
        return this
    }

    /**
     * Returns all scaled sensor values from the most recently fetched frame.
     *
     * If no frame has been fetched yet, an [update] is performed first.
     *
     * @throws PicoBoardException if no frame has been fetched and the read fails.
     */
    @Throws(PicoBoardException::class)
    public fun values(): PicoBoardScaledValues {
        return ensureFrame().scaled
    }

    /**
     * Returns the current slider position as a value from 0 (left) to 100 (right).
     *
     * @throws PicoBoardException if no frame has been fetched and the read fails.
     */
    @Throws(PicoBoardException::class)
    public fun slider(): Int {
        return values().slider
    }

    /**
     * Returns the current sound level as a value from 0 (quiet) to 100 (loud).
     *
     * @throws PicoBoardException if no frame has been fetched and the read fails.
     */
    @Throws(PicoBoardException::class)
    public fun sound(): Int {
        return values().sound
    }

    /**
     * Returns the current light level as a value from 0 (dark) to 100 (bright).
     *
     * @throws PicoBoardException if no frame has been fetched and the read fails.
     */
    @Throws(PicoBoardException::class)
    public fun light(): Int {
        return values().light
    }

    /**
     * Returns `true` if the PicoBoard button is currently pressed.
     *
     * @throws PicoBoardException if no frame has been fetched and the read fails.
     */
    @Throws(PicoBoardException::class)
    public fun buttonPressed(): Boolean {
        return values().buttonPressed
    }

    /**
     * Returns the current resistance on connector A as a value from 0 to 100.
     *
     * @throws PicoBoardException if no frame has been fetched and the read fails.
     */
    @Throws(PicoBoardException::class)
    public fun resistanceA(): Int {
        return values().resistanceA
    }

    /**
     * Returns the current resistance on connector B as a value from 0 to 100.
     *
     * @throws PicoBoardException if no frame has been fetched and the read fails.
     */
    @Throws(PicoBoardException::class)
    public fun resistanceB(): Int {
        return values().resistanceB
    }

    /**
     * Returns the current resistance on connector C as a value from 0 to 100.
     *
     * @throws PicoBoardException if no frame has been fetched and the read fails.
     */
    @Throws(PicoBoardException::class)
    public fun resistanceC(): Int {
        return values().resistanceC
    }

    /**
     * Returns the current resistance on connector D as a value from 0 to 100.
     *
     * @throws PicoBoardException if no frame has been fetched and the read fails.
     */
    @Throws(PicoBoardException::class)
    public fun resistanceD(): Int {
        return values().resistanceD
    }

    /**
     * Reads sensor values in a loop, calling [action] on each iteration, and sleeping
     * [intervalMillis] between iterations.
     *
     * This loop runs forever. Call it from a thread you are happy to block, or cancel it by
     * interrupting the thread.
     *
     * Transient read failures are retried up to [PicoBoardOptions.pollingReadFailureRetries]
     * consecutive times before a [PicoBoardException] is thrown.
     *
     * @param intervalMillis delay between iterations in milliseconds. 0 means no delay.
     *   Defaults to [PicoBoardOptions.pollingInterval].
     * @param action block to execute on each iteration, with this project as receiver.
     * @throws PicoBoardException if reads fail beyond the configured retry budget.
     */
    @JvmOverloads
    @Throws(PicoBoardException::class)
    public fun every(
        intervalMillis: Long = connection.options.pollingInterval.toMillis(),
        action: PicoBoardProject.() -> Unit,
    ): Unit {
        require(intervalMillis >= 0L) {
            "intervalMillis must be zero or greater"
        }

        while (true) {
            readFrameForLoop()
            action()
            sleepIfNeeded(intervalMillis)
        }
    }

    /**
     * Reads sensor values [times] times, calling [action] on each iteration, and sleeping
     * [intervalMillis] between iterations (but not after the last one).
     *
     * Transient read failures are retried up to [PicoBoardOptions.pollingReadFailureRetries]
     * consecutive times before a [PicoBoardException] is thrown.
     *
     * @param times number of iterations to execute. Must be zero or greater.
     * @param intervalMillis delay between iterations in milliseconds. 0 means no delay.
     *   Defaults to [PicoBoardOptions.pollingInterval].
     * @param action block to execute on each iteration, with this project as receiver.
     * @throws PicoBoardException if reads fail beyond the configured retry budget.
     */
    @JvmOverloads
    @Throws(PicoBoardException::class)
    public fun repeat(
        times: Int,
        intervalMillis: Long = connection.options.pollingInterval.toMillis(),
        action: PicoBoardProject.() -> Unit,
    ): Unit {
        require(times >= 0) {
            "times must be zero or greater"
        }
        require(intervalMillis >= 0L) {
            "intervalMillis must be zero or greater"
        }

        kotlin.repeat(times) { index ->
            readFrameForLoop()
            action()
            if (index < times - 1) {
                sleepIfNeeded(intervalMillis)
            }
        }
    }

    /**
     * Starts polling the PicoBoard in a background thread at [intervalMillis] and calls
     * [action] on each successful read.
     *
     * The returned [PicoBoardPollingHandle] can be used to stop polling and check for
     * failures. Polling also stops automatically if more than
     * [PicoBoardOptions.pollingReadFailureRetries] consecutive reads fail, in which case
     * [PicoBoardPollingHandle.failure] will return the cause.
     *
     * Only one polling session may be active at a time per connection.
     *
     * @param intervalMillis polling interval in milliseconds. Must be greater than zero.
     *   Defaults to [PicoBoardOptions.pollingInterval].
     * @param action block to execute on each successful read, with this project as receiver.
     * @return a handle to control or observe the polling session.
     */
    @JvmOverloads
    public fun startPolling(
        intervalMillis: Long = connection.options.pollingInterval.toMillis(),
        action: PicoBoardProject.() -> Unit,
    ): PicoBoardPollingHandle {
        require(intervalMillis > 0L) {
            "intervalMillis must be greater than zero"
        }

        return connection.startPolling(
            listener = PicoBoardListener { frame ->
                lastFrame = frame
                this.action()
            },
            interval = Duration.ofMillis(intervalMillis),
        )
    }

    /**
     * Closes the connection to the PicoBoard.
     *
     * Any active polling session is stopped first. Calling [close] more than once is safe.
     */
    public override fun close() {
        connection.close()
    }

    @Throws(PicoBoardException::class)
    private fun ensureFrame(): PicoBoardFrame {
        val cachedFrame = lastFrame
        if (cachedFrame != null) {
            return cachedFrame
        }
        return update().lastFrame ?: error("PicoBoard frame was not stored")
    }

    @Throws(PicoBoardException::class)
    private fun readFrameForLoop(): PicoBoardFrame {
        var consecutiveFailures = 0
        while (true) {
            try {
                return update().lastFrame ?: error("PicoBoard frame was not stored")
            } catch (failure: PicoBoardException) {
                consecutiveFailures += 1
                if (consecutiveFailures > connection.options.pollingReadFailureRetries) {
                    throw PicoBoardException(
                        "PicoBoard polling failed after ${connection.options.pollingReadFailureRetries} read retries on '$portIdentifier'",
                        failure,
                    )
                }
            }
        }
    }

    private fun sleepIfNeeded(intervalMillis: Long): Unit {
        if (intervalMillis > 0L) {
            sleeper(intervalMillis)
        }
    }
}
