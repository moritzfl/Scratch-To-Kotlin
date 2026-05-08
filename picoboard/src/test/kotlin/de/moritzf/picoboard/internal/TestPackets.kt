package de.moritzf.picoboard.internal

internal fun buildPacket(channelValues: List<Pair<Int, Int>>): ByteArray {
    return channelValues
        .flatMap { (channel, value) ->
            val high = 0x80 or (channel shl 3) or ((value shr 7) and 0x07)
            val low = value and 0x7F
            listOf(high.toByte(), low.toByte())
        }
        .toByteArray()
}
