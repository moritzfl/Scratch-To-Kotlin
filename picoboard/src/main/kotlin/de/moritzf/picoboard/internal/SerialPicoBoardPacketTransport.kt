package de.moritzf.picoboard.internal

import com.fazecast.jSerialComm.SerialPort
import de.moritzf.picoboard.PicoBoardException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.max

internal class SerialPicoBoardPacketTransport private constructor(
    private val port: SerialPort,
    private val input: InputStream,
    private val output: OutputStream,
    private val options: de.moritzf.picoboard.PicoBoardOptions,
    override val identifier: String,
) : PicoBoardPacketTransport {
    private val lock: Any = Any()
    private var closed: Boolean = false

    override fun requestPacket(): ByteArray {
        synchronized(lock) {
            ensureOpen()

            try {
                port.flushIOBuffers()
                output.write(PicoBoardProtocol.POLL_REQUEST)
                output.flush()

                val deadlineNanos = System.nanoTime() + options.readTimeout.toNanos()
                val packet = ByteArray(PicoBoardProtocol.PACKET_SIZE)
                packet[0] = readStartByte(deadlineNanos)
                readFully(packet, 1, PicoBoardProtocol.PACKET_SIZE - 1, deadlineNanos)
                return packet
            } catch (failure: IOException) {
                throw PicoBoardException("Failed to read from PicoBoard on '$identifier'", failure)
            }
        }
    }

    override fun close(): Unit {
        synchronized(lock) {
            if (closed) {
                return
            }
            closed = true

            var failure: Throwable? = null

            try {
                input.close()
            } catch (closeFailure: Throwable) {
                failure = closeFailure
            }

            try {
                output.close()
            } catch (closeFailure: Throwable) {
                if (failure == null) {
                    failure = closeFailure
                }
            }

            if (!port.closePort() && failure == null) {
                failure = PicoBoardException("Failed to close serial port '$identifier'")
            }

            if (failure != null) {
                throw PicoBoardException("Failed to close PicoBoard transport for '$identifier'", failure)
            }
        }
    }

    private fun ensureOpen(): Unit {
        if (closed) {
            throw IllegalStateException("PicoBoard transport '$identifier' is already closed")
        }
    }

    private fun readStartByte(deadlineNanos: Long): Byte {
        while (System.nanoTime() < deadlineNanos) {
            val candidate = readByte(deadlineNanos)
            if (candidate == -1) {
                break
            }

            if ((candidate and 0x80) != 0) {
                val channel = (candidate shr 3) and 0x0F
                if (channel == PicoBoardProtocol.FIRMWARE_CHANNEL) {
                    return candidate.toByte()
                }
            }
        }

        throw PicoBoardException("Timed out waiting for PicoBoard response header on '$identifier'")
    }

    private fun readFully(
        target: ByteArray,
        offset: Int,
        length: Int,
        deadlineNanos: Long,
    ): Unit {
        var cursor = offset
        val end = offset + length
        while (cursor < end) {
            val remainingMillis = remainingMillis(deadlineNanos)
            if (remainingMillis <= 0) {
                throw PicoBoardException("Timed out waiting for PicoBoard payload on '$identifier'")
            }

            port.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, remainingMillis, 0)
            val read = input.read(target, cursor, end - cursor)
            if (read < 0) {
                throw PicoBoardException("Timed out waiting for PicoBoard payload on '$identifier'")
            }
            if (read == 0) {
                continue
            }
            cursor += read
        }
    }

    private fun readByte(deadlineNanos: Long): Int {
        val remainingMillis = remainingMillis(deadlineNanos)
        if (remainingMillis <= 0) {
            return -1
        }

        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, remainingMillis, 0)
        return input.read()
    }

    private fun remainingMillis(deadlineNanos: Long): Int {
        val remainingNanos = deadlineNanos - System.nanoTime()
        if (remainingNanos <= 0) {
            return 0
        }
        return max(1L, remainingNanos / 1_000_000L).toInt()
    }

    internal companion object {
        @Throws(PicoBoardException::class)
        internal fun open(
            identifier: String,
            options: de.moritzf.picoboard.PicoBoardOptions,
        ): SerialPicoBoardPacketTransport {
            try {
                val port = SerialPort.getCommPort(identifier)
                port.setComPortParameters(
                    PicoBoardProtocol.BAUD_RATE,
                    PicoBoardProtocol.DATA_BITS,
                    SerialPort.ONE_STOP_BIT,
                    SerialPort.NO_PARITY,
                )
                port.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED)
                port.setComPortTimeouts(
                    SerialPort.TIMEOUT_READ_BLOCKING,
                    max(1L, options.readTimeout.toMillis()).toInt(),
                    0,
                )

                if (!port.openPort()) {
                    throw PicoBoardException("Unable to open serial port '$identifier'")
                }

                return SerialPicoBoardPacketTransport(
                    port = port,
                    input = port.inputStream,
                    output = port.outputStream,
                    options = options,
                    identifier = identifier,
                )
            } catch (failure: IOException) {
                throw PicoBoardException("Failed to open serial port '$identifier'", failure)
            }
        }
    }
}
