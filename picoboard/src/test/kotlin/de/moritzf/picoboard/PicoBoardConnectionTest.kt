package de.moritzf.picoboard

import de.moritzf.picoboard.internal.PicoBoardPacketTransport
import de.moritzf.picoboard.internal.buildPacket
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PicoBoardConnectionTest {
    @Test
    fun `readFrame delegates to transport and parser`() {
        val transport = FakePacketTransport(
            mutableListOf(
                buildPacket(
                    listOf(
                        15 to 2,
                        0 to 100,
                        1 to 200,
                        2 to 300,
                        3 to 1023,
                        4 to 400,
                        5 to 500,
                        6 to 600,
                        7 to 700,
                    ),
                ),
            ),
        )

        PicoBoardConnection(
            transport = transport,
            portIdentifier = "fake",
            options = PicoBoardOptions(),
        ).use { connection ->
            val frame = connection.readFrame()
            assertEquals(400, frame.raw.resistanceA)
            assertEquals(700, frame.raw.slider)
            assertTrue(!frame.scaled.buttonPressed)
        }
    }

    @Test
    fun `polling produces frames and stays healthy`() {
        val transport = FakePacketTransport(
            MutableList(4) {
                buildPacket(
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
            },
        )

        PicoBoardConnection(
            transport = transport,
            portIdentifier = "fake",
            options = PicoBoardOptions(pollingInterval = Duration.ofMillis(5)),
        ).use { connection ->
            val latch = CountDownLatch(2)
            val handle = connection.startPolling(
                listener = PicoBoardListener {
                    latch.countDown()
                },
            )

            assertTrue(latch.await(1, TimeUnit.SECONDS))
            handle.stop()
            assertNull(handle.failure())
        }
    }

    @Test
    fun `polling retries transient read failures and recovers`() {
        val latch = CountDownLatch(1)
        val transport = FakePacketTransport(
            mutableListOf(
                PicoBoardException("transient 1"),
                PicoBoardException("transient 2"),
                buildPacket(
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
                ),
            ),
        )

        PicoBoardConnection(
            transport = transport,
            portIdentifier = "fake",
            options = PicoBoardOptions(
                pollingInterval = Duration.ofMillis(5),
                pollingReadFailureRetries = 2,
            ),
        ).use { connection ->
            val handle = connection.startPolling(
                listener = PicoBoardListener {
                    latch.countDown()
                },
            )

            assertTrue(latch.await(1, TimeUnit.SECONDS))
            handle.stop()
            assertNull(handle.failure())
        }
    }

    @Test
    fun `polling fails after exhausting read retries`() {
        val transport = FakePacketTransport(
            mutableListOf(
                PicoBoardException("transient 1"),
                PicoBoardException("transient 2"),
                PicoBoardException("transient 3"),
            ),
        )

        PicoBoardConnection(
            transport = transport,
            portIdentifier = "fake",
            options = PicoBoardOptions(
                pollingInterval = Duration.ofMillis(5),
                pollingReadFailureRetries = 2,
            ),
        ).use { connection ->
            val handle = connection.startPolling(PicoBoardListener { })
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1)

            while (handle.failure() == null && System.nanoTime() < deadline) {
                Thread.sleep(10)
            }

            val failure = handle.failure()
            assertIs<PicoBoardException>(failure)
            assertEquals(
                "PicoBoard polling failed after 2 read retries on 'fake'",
                failure.message,
            )
        }
    }
}

private class FakePacketTransport(
    private val responses: MutableList<Any>,
) : PicoBoardPacketTransport {
    override val identifier: String = "fake"

    override fun requestPacket(): ByteArray {
        return when (val response = responses.removeFirstOrNull()) {
            null -> throw PicoBoardException("No more packets available")
            is ByteArray -> response
            is PicoBoardException -> throw response
            else -> error("Unsupported fake transport response: ${response::class.qualifiedName}")
        }
    }

    override fun close(): Unit = Unit
}
