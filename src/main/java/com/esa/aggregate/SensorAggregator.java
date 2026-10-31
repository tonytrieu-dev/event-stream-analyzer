package com.esa.aggregate;

import com.esa.model.Record;
import com.esa.model.RecordType;
import com.esa.model.SensorReading;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SensorAggregator implements Aggregator
{
    private final Map<String, List<Double>> readingsBySensor = new LinkedHashMap<>();

    @Override
    public RecordType handles()
    {
        return RecordType.SENSOR;
    }

    @Override
    public void accept(Record record)
    {
        SensorReading reading = (SensorReading) record;
        readingsBySensor
                .computeIfAbsent(reading.sensor(), key -> new ArrayList<>())
                .add(reading.reading());
    }

    @Override
    public Map<String, Object> result()
    {
        Map<String, Object> bySensor = new LinkedHashMap<>();
        for (Map.Entry<String, List<Double>> entry : readingsBySensor.entrySet())
        {
            bySensor.put(entry.getKey(), summarize(entry.getValue()));
        }
        return bySensor;
    }

    private static Map<String, Object> summarize(List<Double> readings)
    {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("count", readings.size());
        stats.put("minimum", round(Collections.min(readings)));
        stats.put("median", round(Stats.median(readings)));
        stats.put("average", round(Stats.mean(readings)));
        stats.put("maximum", round(Collections.max(readings)));
        stats.put("std_dev", round(Stats.populationStdDev(readings)));
        return stats;
    }

    private static double round(double value)
    {
        return Math.round(value * 10.0) / 10.0;
    }
}
