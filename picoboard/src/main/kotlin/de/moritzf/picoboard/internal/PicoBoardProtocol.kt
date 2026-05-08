package de.moritzf.picoboard.internal

internal object PicoBoardProtocol {
    internal const val BAUD_RATE: Int = 38_400
    internal const val DATA_BITS: Int = 8
    internal const val PACKET_SIZE: Int = 18
    internal const val CHANNEL_COUNT: Int = 16
    internal const val FIRMWARE_CHANNEL: Int = 15
    internal const val POLL_REQUEST: Int = 0x01
    internal const val MAX_SENSOR_VALUE: Int = 1023
}
