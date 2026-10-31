package com.esa.aggregate;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.esa.model.SensorReading;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SensorAggregatorTest
{
    private final SensorAggregator aggregator = new SensorAggregator();

    private void accept(String sensor, double reading)
    {
        aggregator.accept(new SensorReading(sensor, reading));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> statsFor(String sensor)
    {
        return (Map<String, Object>) aggregator.result().get(sensor);
    }

    @Test
    void groupsReadingsByArbitrarySensorNameAndCountsThemSeparately()
    {
        accept("temp", 10.0);
        accept("temp", 20.0);
        accept("a_brand_new_sensor", 99.0);

        assertEquals(2, statsFor("temp").get("count"));
        assertEquals(1, statsFor("a_brand_new_sensor").get("count"),
                "sensors are grouped by whatever name appears, not a fixed set");
    }

    @Test
    void reportsTheMinimumAndMaximumReading()
    {
        accept("temp", 10.0);
        accept("temp", 40.0);
        accept("temp", 25.0);

        assertEquals(10.0, statsFor("temp").get("minimum"));
        assertEquals(40.0, statsFor("temp").get("maximum"));
    }

    @Test
    void wiresEachStatToItsCorrectOutputKey()
    {
        // Distinct values so a swapped key (e.g. median mapped to average) is visible.
        accept("temp", 10.0);
        accept("temp", 20.0);
        accept("temp", 60.0);

        Map<String, Object> stats = statsFor("temp");
        assertEquals(20.0, stats.get("median"), "median key must carry the median");
        assertEquals(30.0, stats.get("average"), "average key must carry the mean");
    }
}
