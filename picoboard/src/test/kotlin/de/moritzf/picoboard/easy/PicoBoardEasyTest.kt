package de.moritzf.picoboard.easy

import de.moritzf.picoboard.PicoBoardConnection
import de.moritzf.picoboard.PicoBoardException
import de.moritzf.picoboard.PicoBoardOptions
import de.moritzf.picoboard.internal.PicoBoardPacketTransport
import de.moritzf.picoboard.internal.buildPacket
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PicoBoardEasyTest {
    @Test
    fun `sensor getters read one frame and then reuse it`() {
        val transport = FakePacketTransport(
            mutableListOf(
                buildPacket(
                    listOf(
                        15 to 4,
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

        createProject(transport).use { project ->
            assertEquals(68, project.slider())
            assertEquals(39, project.resistanceA())
            assertFalse(project.buttonPressed())
            assertEquals(0, transport.remainingResponses())
        }
    }

    @Test
    fun `repeat retries transient read failures`() {
        val transport = FakePacketTransport(
            mutableListOf(
                PicoBoardException("temporary read problem"),
                buildPacket(
                    listOf(
                        15 to 4,
                        0 to 100,
                        1 to 200,
                        2 to 300,
                        3 to 0,
                        4 to 400,
                        5 to 500,
                        6 to 600,
                        7 to 700,
                    ),
                ),
                buildPacket(
                    listOf(
                        15 to 4,
                        0 to 101,
                        1 to 201,
                        2 to 301,
                        3 to 1023,
                        4 to 401,
                        5 to 501,
                        6 to 601,
                        7 to 701,
                    ),
                ),
            ),
        )

        val sliderValues = mutableListOf<Int>()
        createProject(
            transport = transport,
            options = PicoBoardOptions(
                pollingInterval = Duration.ofMillis(5),
                pollingReadFailureRetries = 2,
            ),
        ).use { project ->
            project.repeat(times = 2, intervalMillis = 0) {
                sliderValues += slider()
            }
        }

        assertEquals(listOf(68, 69), sliderValues)
    }

    @Test
    fun `repeat fails after polling retry budget is exhausted`() {
        val transport = FakePacketTransport(
            mutableListOf(
                PicoBoardException("temporary 1"),
                PicoBoardException("temporary 2"),
                PicoBoardException("temporary 3"),
            ),
        )

        createProject(
            transport = transport,
            options = PicoBoardOptions(
                pollingInterval = Duration.ofMillis(5),
                pollingReadFailureRetries = 2,
            ),
        ).use { project ->
            val failure = assertFailsWith<PicoBoardException> {
                project.repeat(times = 1, intervalMillis = 0) { }
            }

            assertEquals(
                "PicoBoard polling failed after 2 read retries on 'fake'",
                failure.message,
            )
        }
    }

    @Test
    fun `startPolling keeps easy getters updated`() {
        val transport = FakePacketTransport(
            mutableListOf(
                buildPacket(
                    listOf(
                        15 to 4,
                        0 to 100,
                        1 to 200,
                        2 to 300,
                        3 to 0,
                        4 to 400,
                        5 to 500,
                        6 to 600,
                        7 to 700,
                    ),
                ),
                buildPacket(
                    listOf(
                        15 to 4,
                        0 to 101,
                        1 to 201,
                        2 to 301,
                        3 to 1023,
                        4 to 401,
                        5 to 501,
                        6 to 601,
                        7 to 701,
                    ),
                ),
            ),
        )

        val sliderValues = mutableListOf<Int>()
        val buttonValues = mutableListOf<Boolean>()
        val latch = CountDownLatch(2)

        createProject(
            transport = transport,
            options = PicoBoardOptions(
                pollingInterval = Duration.ofMillis(5),
                pollingReadFailureRetries = 2,
            ),
        ).use { project ->
            val handle = project.startPolling(intervalMillis = 5) {
                sliderValues += slider()
                buttonValues += buttonPressed()
                latch.countDown()
            }

            assertTrue(latch.await(1, TimeUnit.SECONDS))
            handle.stop()
            assertNull(handle.failure())
        }

        assertEquals(listOf(68, 69), sliderValues)
        assertEquals(listOf(true, false), buttonValues)
    }

    @Test
    fun `startService delivers initial values immediately`() {
        val transport = FakePacketTransport(
            mutableListOf(
                buildPacket(
                    listOf(
                        15 to 4,
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

        createService(transport).use { service ->
            assertEquals(68, service.slider())
            assertFalse(service.buttonPressed())
            assertTrue(service.isRunning())
        }
    }

    @Test
    fun `startService keeps values updated through background polling`() {
        val latch = CountDownLatch(2)
        val transport = FakePacketTransport(
            responses = mutableListOf(
                buildPacket(
                    listOf(
                        15 to 4,
                        0 to 100,
                        1 to 200,
                        2 to 300,
                        3 to 0,
                        4 to 400,
                        5 to 500,
                        6 to 600,
                        7 to 700,
                    ),
                ),
                buildPacket(
                    listOf(
                        15 to 4,
                        0 to 101,
                        1 to 201,
                        2 to 301,
                        3 to 1023,
                        4 to 401,
                        5 to 501,
                        6 to 601,
                        7 to 701,
                    ),
                ),
            ),
            onPacketRequested = { latch.countDown() },
        )

        createService(
            transport = transport,
            options = PicoBoardOptions(
                pollingInterval = Duration.ofMillis(5),
                pollingReadFailureRetries = 2,
            ),
            intervalMillis = 5,
        ).use { service ->
            assertEquals(68, service.slider())
            assertTrue(service.buttonPressed())

            assertTrue(latch.await(1, TimeUnit.SECONDS))
            Thread.sleep(20) // let cachedValues be written after the second packet

            assertEquals(69, service.slider())
            assertFalse(service.buttonPressed())
        }
    }

    private fun createProject(
        transport: FakePacketTransport,
        options: PicoBoardOptions = PicoBoardOptions(),
    ): PicoBoardProject {
        return PicoBoardProject(
            connection = PicoBoardConnection(
                transport = transport,
                portIdentifier = "fake",
                options = options,
            ),
            sleeper = { },
        )
    }

    private fun createService(
        transport: FakePacketTransport,
        options: PicoBoardOptions = PicoBoardOptions(),
        intervalMillis: Long = options.pollingInterval.toMillis(),
    ): PicoBoardService {
        return PicoBoardService(
            project = PicoBoardProject(
                connection = PicoBoardConnection(
                    transport = transport,
                    portIdentifier = "fake",
                    options = options,
                ),
            ),
            intervalMillis = intervalMillis,
        )
    }
}

private class FakePacketTransport(
    private val responses: MutableList<Any>,
    private val onPacketRequested: (() -> Unit)? = null,
) : PicoBoardPacketTransport {
    override val identifier: String = "fake"

    override fun requestPacket(): ByteArray {
        val result = when (val response = responses.removeFirstOrNull()) {
            null -> throw PicoBoardException("No more packets available")
            is ByteArray -> response
            is PicoBoardException -> throw response
            else -> error("Unsupported fake transport response: ${response::class.qualifiedName}")
        }
        onPacketRequested?.invoke()
        return result
    }

    fun remainingResponses(): Int {
        return responses.size
    }

    override fun close(): Unit = Unit
}
