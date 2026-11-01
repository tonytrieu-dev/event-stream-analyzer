package com.esa.aggregate;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.esa.model.DeviceEvent;
import com.esa.model.Severity;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DeviceAggregatorTest
{
    private final DeviceAggregator aggregator = new DeviceAggregator();

    private void accept(String device, Severity severity)
    {
        aggregator.accept(new DeviceEvent(device, severity));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> bySeverity()
    {
        return (Map<String, Object>) aggregator.result().get("by_severity");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> device(String name)
    {
        Map<String, Object> byDevice = (Map<String, Object>) aggregator.result().get("by_device");
        return (Map<String, Object>) byDevice.get(name);
    }

    @Test
    void countsEventsPerSeverityLevel()
    {
        accept("a", Severity.INFO);
        accept("b", Severity.INFO);
        accept("c", Severity.ERROR);

        assertEquals(2, bySeverity().get("INFO"));
        assertEquals(1, bySeverity().get("ERROR"));
    }

    @Test
    void breaksATieByChoosingTheHighestSeverityRegardlessOfOrderSeen()
    {
        // INFO is accepted first but ERROR is higher severity, so ERROR must win
        // the count tie. Seeing INFO first means a naive first-seen tie-break fails.
        accept("door-lock-07", Severity.INFO);
        accept("door-lock-07", Severity.ERROR);

        assertEquals("ERROR", device("door-lock-07").get("most_frequent_severity"),
                "on a count tie, CRITICAL > ERROR > WARN > INFO decides the winner");
    }

    @Test
    void countOutweighsSeverityWhenCountsDiffer()
    {
        // INFO (lowest severity) occurs twice vs CRITICAL (highest) once: count wins,
        // so a "just pick the highest severity" bug would wrongly return CRITICAL.
        accept("sensor-hub", Severity.INFO);
        accept("sensor-hub", Severity.INFO);
        accept("sensor-hub", Severity.CRITICAL);

        assertEquals(3, device("sensor-hub").get("total"));
        assertEquals("INFO", device("sensor-hub").get("most_frequent_severity"),
                "a higher count must beat a higher severity");
    }
}
