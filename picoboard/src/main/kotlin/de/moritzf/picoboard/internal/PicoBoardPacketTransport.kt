package de.moritzf.picoboard.internal

import de.moritzf.picoboard.PicoBoardException

internal interface PicoBoardPacketTransport : AutoCloseable {
    val identifier: String

    @Throws(PicoBoardException::class)
    fun requestPacket(): ByteArray

    @Throws(PicoBoardException::class)
    override fun close()
}
