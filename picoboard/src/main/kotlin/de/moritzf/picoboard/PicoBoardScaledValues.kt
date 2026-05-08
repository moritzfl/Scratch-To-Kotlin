package de.moritzf.picoboard

import de.moritzf.picoboard.internal.PicoBoardPacketParser

public data class PicoBoardScaledValues(
    val resistanceA: Int,
    val resistanceB: Int,
    val resistanceC: Int,
    val resistanceD: Int,
    val slider: Int,
    val sound: Int,
    val light: Int,
    val buttonPressed: Boolean,
) {
    public companion object {
        @JvmStatic
        public fun fromRaw(raw: PicoBoardRawValues): PicoBoardScaledValues {
            return PicoBoardPacketParser.toScaledValues(raw)
        }
    }
}
