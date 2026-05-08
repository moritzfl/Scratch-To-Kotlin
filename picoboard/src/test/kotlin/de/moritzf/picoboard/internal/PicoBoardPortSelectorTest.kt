package de.moritzf.picoboard.internal

import de.moritzf.picoboard.PicoBoardPort
import de.moritzf.picoboard.PicoBoardPortSelectionException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class PicoBoardPortSelectorTest {
    @Test
    fun `autoSelect prefers a unique FTDI style port`() {
        val selected = PicoBoardPortSelector.autoSelect(
            listOf(
                port("/dev/cu.Bluetooth-Incoming-Port", "Bluetooth-Incoming-Port", "Bluetooth", "Bluetooth"),
                port("/dev/cu.usbserial-A5061E1Q", "cu.usbserial-A5061E1Q", "FT232R USB UART", "FT232R USB UART"),
                port("/dev/cu.SonosAce", "SonosAce", "SonosAce", "SonosAce"),
            ),
        )

        assertEquals("/dev/cu.usbserial-A5061E1Q", selected?.systemPortPath)
    }

    @Test
    fun `autoSelect prefers macOS cu device over matching tty device`() {
        val selected = PicoBoardPortSelector.autoSelect(
            listOf(
                port("/dev/tty.usbserial-A5061E1Q", "tty.usbserial-A5061E1Q", "FT232R USB UART (Dial-In)", "FT232R USB UART (Dial-In)"),
                port("/dev/cu.usbserial-A5061E1Q", "cu.usbserial-A5061E1Q", "FT232R USB UART", "FT232R USB UART"),
            ),
        )

        assertEquals("/dev/cu.usbserial-A5061E1Q", selected?.systemPortPath)
    }

    @Test
    fun `autoSelect returns null for ambiguous best match`() {
        val selected = PicoBoardPortSelector.autoSelect(
            listOf(
                port("/dev/cu.usbserial-A", "cu.usbserial-A", "FT232R USB UART", "FT232R USB UART"),
                port("/dev/cu.usbserial-B", "cu.usbserial-B", "FT232R USB UART", "FT232R USB UART"),
            ),
        )

        assertNull(selected)
    }

    @Test
    fun `requireAutoSelected fails when no suitable port exists`() {
        val failure = assertFailsWith<PicoBoardPortSelectionException> {
            PicoBoardPortSelector.requireAutoSelected(
                listOf(
                    port("/dev/cu.Bluetooth-Incoming-Port", "Bluetooth-Incoming-Port", "Bluetooth", "Bluetooth"),
                ),
            )
        }

        assertEquals(
            "No suitable PicoBoard serial port found. Detected ports: /dev/cu.Bluetooth-Incoming-Port",
            failure.message,
        )
    }

    @Test
    fun `requireAutoSelected fails when best match is ambiguous`() {
        val failure = assertFailsWith<PicoBoardPortSelectionException> {
            PicoBoardPortSelector.requireAutoSelected(
                listOf(
                    port("/dev/cu.usbserial-A", "cu.usbserial-A", "FT232R USB UART", "FT232R USB UART"),
                    port("/dev/cu.usbserial-B", "cu.usbserial-B", "FT232R USB UART", "FT232R USB UART"),
                ),
            )
        }

        assertEquals(
            "Multiple suitable PicoBoard serial ports found: /dev/cu.usbserial-A, /dev/cu.usbserial-B",
            failure.message,
        )
    }

    private fun port(
        systemPortPath: String,
        systemPortName: String,
        descriptivePortName: String,
        portDescription: String,
    ): PicoBoardPort {
        return PicoBoardPort(
            systemPortPath = systemPortPath,
            systemPortName = systemPortName,
            descriptivePortName = descriptivePortName,
            portDescription = portDescription,
        )
    }
}
