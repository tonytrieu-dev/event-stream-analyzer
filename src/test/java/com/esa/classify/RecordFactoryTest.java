package com.esa.classify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.esa.model.AccessRecord;
import com.esa.model.DeviceEvent;
import com.esa.model.Record;
import com.esa.model.SensorReading;
import com.esa.model.Severity;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RecordFactoryTest
{
    private final RecordFactory factory = new RecordFactory();

    @Test
    void classifiesASensorReadingWhenSensorAndNumericReadingArePresent()
    {
        Optional<Record> classified =
                factory.classify(Map.of("sensor", "temperature_celsius", "reading", "21.5"));

        SensorReading reading =
                assertInstanceOf(SensorReading.class, classified.orElseThrow());
        assertEquals("temperature_celsius", reading.sensor());
        assertEquals(21.5, reading.reading());
    }

    @Test
    void skipsASensorLineWhenTheReadingIsNotNumeric()
    {
        Optional<Record> classified =
                factory.classify(Map.of("sensor", "temperature_celsius", "reading", "abc"));

        assertTrue(classified.isEmpty(),
                "a non-numeric reading must not classify as a sensor reading (FR-7)");
    }

    @Test
    void classifiesADeviceEventWhenSeverityAndDeviceArePresent()
    {
        Optional<Record> classified =
                factory.classify(Map.of("severity", "ERROR", "device", "door-lock-07"));

        DeviceEvent event = assertInstanceOf(DeviceEvent.class, classified.orElseThrow());
        assertEquals("door-lock-07", event.device());
        assertEquals(Severity.ERROR, event.severity());
    }

    @Test
    void skipsADeviceLineWhenTheSeverityIsNotAKnownLevel()
    {
        Optional<Record> classified =
                factory.classify(Map.of("severity", "FOO", "device", "door-lock-07"));

        assertTrue(classified.isEmpty(),
                "a severity outside CRITICAL/ERROR/WARN/INFO must not classify (FR-7)");
    }

    @Test
    void classifiesAnAccessRecordWhenAllFourKeysAreValid()
    {
        Optional<Record> classified = factory.classify(Map.of(
                "user", "user-42",
                "door", "door-A",
                "result", "DENIED",
                "duration_ms", "30"));

        AccessRecord access = assertInstanceOf(AccessRecord.class, classified.orElseThrow());
        assertEquals("user-42", access.user());
        assertEquals("door-A", access.door());
        assertEquals(false, access.granted(), "DENIED must map to granted=false");
        assertEquals(30, access.durationMs());
    }

    @Test
    void skipsAnAccessLineWhenDurationIsNotAnInteger()
    {
        Optional<Record> classified = factory.classify(Map.of(
                "user", "user-42",
                "door", "door-A",
                "result", "DENIED",
                "duration_ms", "12.5"));

        assertTrue(classified.isEmpty(),
                "a non-integer duration_ms must not classify as an access record (FR-7)");
    }

    @Test
    void skipsACorruptLineWithoutThrowing()
    {
        Optional<Record> classified = factory.classify(Map.of("###CORRUPTED###", "!!!"));

        assertTrue(classified.isEmpty(),
                "a line matching no type must be skipped silently, not throw (FR-7)");
    }

    @Test
    void ignoresExtraKeysBeyondThoseRequired()
    {
        Optional<Record> classified = factory.classify(Map.of(
                "severity", "ERROR",
                "device", "door-lock-07",
                "msg", "Latch jammed",
                "code", "E41"));

        DeviceEvent event = assertInstanceOf(DeviceEvent.class, classified.orElseThrow());
        assertEquals("door-lock-07", event.device(),
                "extra keys must be ignored, not block classification (FR-8)");
    }
}
