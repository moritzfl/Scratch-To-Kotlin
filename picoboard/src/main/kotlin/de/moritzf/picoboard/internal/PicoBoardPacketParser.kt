package de.moritzf.picoboard.internal

import de.moritzf.picoboard.PicoBoardFrame
import de.moritzf.picoboard.PicoBoardProtocolException
import de.moritzf.picoboard.PicoBoardRawValues
import de.moritzf.picoboard.PicoBoardScaledValues
import java.time.Instant
import kotlin.math.min
import kotlin.math.roundToInt

internal object PicoBoardPacketParser {
    private const val MISSING_VALUE: Int = Int.MIN_VALUE

    internal fun parse(packet: ByteArray, timestamp: Instant): PicoBoardFrame {
        if (packet.size != PicoBoardProtocol.PACKET_SIZE) {
            throw PicoBoardProtocolException(
                "Expected ${PicoBoardProtocol.PACKET_SIZE} bytes but received ${packet.size}",
            )
        }

        val channels = IntArray(PicoBoardProtocol.CHANNEL_COUNT) { MISSING_VALUE }
        for (offset in packet.indices step 2) {
            val high = packet[offset].toInt() and 0xFF
            val low = packet[offset + 1].toInt() and 0xFF
            val pairIndex = offset / 2

            if ((high and 0x80) == 0) {
                throw PicoBoardProtocolException("Invalid high byte at pair $pairIndex: 0x${high.toString(16)}")
            }
            if ((low and 0x80) != 0) {
                throw PicoBoardProtocolException("Invalid low byte at pair $pairIndex: 0x${low.toString(16)}")
            }

            val channel = (high shr 3) and 0x0F
            if (channel != PicoBoardProtocol.FIRMWARE_CHANNEL && channel !in 0..7) {
                throw PicoBoardProtocolException("Unexpected PicoBoard channel $channel")
            }
            if (channels[channel] != MISSING_VALUE) {
                throw PicoBoardProtocolException("Duplicate PicoBoard channel $channel in packet")
            }

            channels[channel] = ((high and 0x07) shl 7) or (low and 0x7F)
        }

        if (channels[PicoBoardProtocol.FIRMWARE_CHANNEL] == MISSING_VALUE) {
            throw PicoBoardProtocolException("Firmware channel ${PicoBoardProtocol.FIRMWARE_CHANNEL} is missing")
        }

        for (channel in 0..7) {
            if (channels[channel] == MISSING_VALUE) {
                throw PicoBoardProtocolException("Sensor channel $channel is missing")
            }
        }

        val raw = PicoBoardRawValues(
            resistanceA = channels[4],
            resistanceB = channels[2],
            resistanceC = channels[1],
            resistanceD = channels[0],
            slider = channels[7],
            sound = channels[6],
            light = channels[5],
            button = channels[3],
        )

        return PicoBoardFrame(
            timestamp = timestamp,
            firmwareId = channels[PicoBoardProtocol.FIRMWARE_CHANNEL],
            raw = raw,
            scaled = toScaledValues(raw),
        )
    }

    internal fun toScaledValues(raw: PicoBoardRawValues): PicoBoardScaledValues {
        return PicoBoardScaledValues(
            resistanceA = scaleLinear(raw.resistanceA),
            resistanceB = scaleLinear(raw.resistanceB),
            resistanceC = scaleLinear(raw.resistanceC),
            resistanceD = scaleLinear(raw.resistanceD),
            slider = scaleLinear(raw.slider),
            sound = scaleSound(raw.sound),
            light = scaleLight(raw.light),
            buttonPressed = isButtonPressed(raw.button),
        )
    }

    private fun scaleLinear(raw: Int): Int {
        return (raw.clampSensorValue() * 100.0 / PicoBoardProtocol.MAX_SENSOR_VALUE).roundToInt()
    }

    private fun scaleLight(raw: Int): Int {
        val sensorValue = raw.clampSensorValue()
        return if (sensorValue < 25) {
            100 - sensorValue
        } else {
            ((PicoBoardProtocol.MAX_SENSOR_VALUE - sensorValue) * 75.0 / 998.0).roundToInt().coerceIn(0, 75)
        }
    }

    private fun scaleSound(raw: Int): Int {
        val adjusted = (raw.clampSensorValue() - 18).coerceAtLeast(0)
        return if (adjusted < 50) {
            adjusted / 2
        } else {
            25 + min(75, ((adjusted - 50) * 75.0 / 580.0).roundToInt())
        }
    }

    private fun isButtonPressed(raw: Int): Boolean {
        return raw.clampSensorValue() <= 512
    }

    private fun Int.clampSensorValue(): Int {
        return coerceIn(0, PicoBoardProtocol.MAX_SENSOR_VALUE)
    }
}
