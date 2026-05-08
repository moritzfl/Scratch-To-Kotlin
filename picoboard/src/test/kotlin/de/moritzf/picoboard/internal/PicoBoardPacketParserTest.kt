package de.moritzf.picoboard.internal

import de.moritzf.picoboard.PicoBoardProtocolException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PicoBoardPacketParserTest {
    @Test
    fun `parser decodes firmware and sensor channels`() {
        val packet = buildPacket(
            listOf(
                15 to 2,
                0 to 512,
                1 to 256,
                2 to 128,
                3 to 0,
                4 to 1023,
                5 to 25,
                6 to 68,
                7 to 700,
            ),
        )

        val frame = PicoBoardPacketParser.parse(packet, java.time.Instant.parse("2026-04-12T10:00:00Z"))

        assertEquals(2, frame.firmwareId)
        assertEquals(1023, frame.raw.resistanceA)
        assertEquals(128, frame.raw.resistanceB)
        assertEquals(256, frame.raw.resistanceC)
        assertEquals(512, frame.raw.resistanceD)
        assertEquals(700, frame.raw.slider)
        assertEquals(68, frame.raw.sound)
        assertEquals(25, frame.raw.light)
        assertEquals(0, frame.raw.button)
        assertEquals(100, frame.scaled.resistanceA)
        assertEquals(68, frame.scaled.slider)
        assertEquals(25, frame.scaled.sound)
        assertEquals(75, frame.scaled.light)
        assertTrue(frame.scaled.buttonPressed)
    }

    @Test
    fun `parser rejects malformed framing`() {
        val malformedPacket = buildPacket(
            listOf(
                15 to 2,
                0 to 100,
                1 to 100,
                2 to 100,
                3 to 100,
                4 to 100,
                5 to 100,
                6 to 100,
                7 to 100,
            ),
        ).apply {
            this[1] = (this[1].toInt() or 0x80).toByte()
        }

        assertFailsWith<PicoBoardProtocolException> {
            PicoBoardPacketParser.parse(malformedPacket, java.time.Instant.now())
        }
    }

    @Test
    fun `scaling follows scratch style rules`() {
        assertEquals(
            100,
            PicoBoardPacketParser.toScaledValues(
                de.moritzf.picoboard.PicoBoardRawValues(
                    resistanceA = 0,
                    resistanceB = 0,
                    resistanceC = 0,
                    resistanceD = 0,
                    slider = 0,
                    sound = 0,
                    light = 0,
                    button = 1023,
                ),
            ).light,
        )

        assertEquals(
            0,
            PicoBoardPacketParser.toScaledValues(
                de.moritzf.picoboard.PicoBoardRawValues(
                    resistanceA = 0,
                    resistanceB = 0,
                    resistanceC = 0,
                    resistanceD = 0,
                    slider = 0,
                    sound = 18,
                    light = 1023,
                    button = 1023,
                ),
            ).sound,
        )
    }
}
