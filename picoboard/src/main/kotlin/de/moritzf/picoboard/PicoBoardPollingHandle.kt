package de.moritzf.picoboard

public interface PicoBoardPollingHandle : AutoCloseable {
    public fun stop()

    public fun isRunning(): Boolean

    public fun failure(): Throwable?

    public fun throwIfFailed()

    public override fun close() {
        stop()
    }
}
