package de.moritzf.picoboard

import de.moritzf.picoboard.internal.PicoBoardPacketParser
import de.moritzf.picoboard.internal.PicoBoardPacketTransport
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max

public class PicoBoardConnection internal constructor(
    private val transport: PicoBoardPacketTransport,
    public val portIdentifier: String,
    public val options: PicoBoardOptions,
) : AutoCloseable {
    private val closed: AtomicBoolean = AtomicBoolean(false)
    private val stateLock: Any = Any()

    @Volatile
    private var pollingHandle: DefaultPicoBoardPollingHandle? = null

    @Throws(PicoBoardException::class)
    public fun readFrame(): PicoBoardFrame {
        synchronized(stateLock) {
            ensureOpen()
            val packet = transport.requestPacket()
            return PicoBoardPacketParser.parse(packet, Instant.now())
        }
    }

    public fun isOpen(): Boolean {
        return !closed.get()
    }

    @JvmOverloads
    public fun startPolling(
        listener: PicoBoardListener,
        interval: Duration = options.pollingInterval,
    ): PicoBoardPollingHandle {
        require(!interval.isNegative && !interval.isZero) {
            "interval must be greater than zero"
        }

        synchronized(stateLock) {
            ensureOpen()

            val activeHandle = pollingHandle
            if (activeHandle != null && activeHandle.isRunning()) {
                throw IllegalStateException("Polling is already active for '$portIdentifier'")
            }

            val executor = Executors.newSingleThreadScheduledExecutor(PicoBoardThreadFactory(portIdentifier))
            var handleReference: DefaultPicoBoardPollingHandle? = null
            val handle = DefaultPicoBoardPollingHandle(executor) {
                synchronized(stateLock) {
                    if (pollingHandle === handleReference) {
                        pollingHandle = null
                    }
                }
            }
            handleReference = handle
            val consecutiveReadFailures = AtomicInteger(0)

            val task = executor.scheduleWithFixedDelay(
                {
                    try {
                        val frame = try {
                            readFrame().also {
                                consecutiveReadFailures.set(0)
                            }
                        } catch (failure: PicoBoardException) {
                            val failureCount = consecutiveReadFailures.incrementAndGet()
                            if (failureCount > options.pollingReadFailureRetries) {
                                handle.fail(
                                    PicoBoardException(
                                        "PicoBoard polling failed after ${options.pollingReadFailureRetries} read retries on '$portIdentifier'",
                                        failure,
                                    ),
                                )
                            }
                            return@scheduleWithFixedDelay
                        }

                        listener.onFrame(frame)
                    } catch (interrupted: InterruptedException) {
                        Thread.currentThread().interrupt()
                        handle.stop()
                    } catch (failure: Throwable) {
                        handle.fail(failure)
                    }
                },
                0L,
                interval.toSchedulerDelayMillis(),
                TimeUnit.MILLISECONDS,
            )

            handle.attach(task)
            pollingHandle = handle
            return handle
        }
    }

    @Throws(PicoBoardException::class)
    public override fun close() {
        synchronized(stateLock) {
            if (!closed.compareAndSet(false, true)) {
                return
            }

            pollingHandle?.stop()
            pollingHandle = null
            transport.close()
        }
    }

    private fun ensureOpen(): Unit {
        if (closed.get()) {
            throw IllegalStateException("PicoBoard connection '$portIdentifier' is already closed")
        }
    }

    private fun Duration.toSchedulerDelayMillis(): Long {
        return max(1L, toMillis())
    }
}

private class DefaultPicoBoardPollingHandle(
    private val executor: ScheduledExecutorService,
    private val onStop: () -> Unit,
) : PicoBoardPollingHandle {
    private val running: AtomicBoolean = AtomicBoolean(true)
    private val failureRef: AtomicReference<Throwable?> = AtomicReference(null)

    @Volatile
    private var task: ScheduledFuture<*>? = null

    internal fun attach(task: ScheduledFuture<*>): Unit {
        this.task = task
    }

    internal fun fail(failure: Throwable): Unit {
        failureRef.compareAndSet(null, failure)
        stop()
    }

    override fun stop(): Unit {
        if (!running.compareAndSet(true, false)) {
            return
        }

        task?.cancel(true)
        executor.shutdownNow()
        onStop()
    }

    override fun isRunning(): Boolean {
        return running.get()
    }

    override fun failure(): Throwable? {
        return failureRef.get()
    }

    override fun throwIfFailed(): Unit {
        val failure = failureRef.get() ?: return
        throw IllegalStateException("PicoBoard polling failed", failure)
    }
}

private class PicoBoardThreadFactory(
    private val portIdentifier: String,
) : ThreadFactory {
    override fun newThread(runnable: Runnable): Thread {
        return Thread(runnable, "picoboard-poll-${portIdentifier.sanitizeThreadName()}").apply {
            isDaemon = true
        }
    }
}

private fun String.sanitizeThreadName(): String {
    return replace(Regex("[^A-Za-z0-9._-]"), "_")
}
