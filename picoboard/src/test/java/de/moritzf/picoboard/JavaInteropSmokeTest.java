package de.moritzf.picoboard;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

final class JavaInteropSmokeTest {
    @Test
    void publicApiIsUsableFromJava() {
        PicoBoardRawValues raw = new PicoBoardRawValues(1, 2, 3, 4, 5, 6, 7, 1023);
        PicoBoardScaledValues scaled = PicoBoardScaledValues.fromRaw(raw);
        PicoBoardFrame frame = new PicoBoardFrame(Instant.parse("2026-04-12T10:00:00Z"), 2, raw, scaled);
        PicoBoardOptions options = new PicoBoardOptions(Duration.ofMillis(250), Duration.ofMillis(50));
        List<PicoBoardPort> ports = PicoBoard.listPorts();
        PicoBoardPort autoSelectedPort = PicoBoard.findAutoSelectedPort();

        assertNotNull(frame.getRaw());
        assertNotNull(frame.getScaled());
        assertNotNull(options.getReadTimeout());
        assertNotNull(ports);
        if (autoSelectedPort != null) {
            assertNotNull(autoSelectedPort.getSystemPortPath());
        }
    }
}
