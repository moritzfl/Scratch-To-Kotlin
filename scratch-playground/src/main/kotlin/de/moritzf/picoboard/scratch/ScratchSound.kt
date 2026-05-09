package de.moritzf.picoboard.scratch

import korlibs.audio.sound.Sound
import korlibs.audio.sound.SoundChannel
import korlibs.audio.sound.await

/**
 * A sound loaded from the resources folder via [ScratchStage.sound].
 */
public class ScratchSound internal constructor(
    private val sound: Sound,
    private val onChannelStarted: (SoundChannel) -> Unit,
) {
    /**
     * Starts playing the sound and immediately continues running the program.
     *
     * Equivalent to Scratch's "start sound" block.
     */
    public fun play(): SoundChannel {
        return sound.playNoCancel().also(onChannelStarted)
    }

    /**
     * Plays the sound and suspends until playback has finished.
     *
     * Equivalent to Scratch's "play sound until done" block.
     */
    public suspend fun playUntilDone(): Unit {
        sound.playNoCancel().also(onChannelStarted).await()
    }

    /**
     * Starts looping the sound and immediately continues running the program.
     */
    public fun loop(): SoundChannel {
        return sound.playNoCancelForever().also(onChannelStarted)
    }
}
