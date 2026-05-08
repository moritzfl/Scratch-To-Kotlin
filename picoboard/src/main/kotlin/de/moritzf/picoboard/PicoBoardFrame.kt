package de.moritzf.picoboard

import java.time.Instant

public data class PicoBoardFrame(
    val timestamp: Instant,
    val firmwareId: Int,
    val raw: PicoBoardRawValues,
    val scaled: PicoBoardScaledValues,
)
