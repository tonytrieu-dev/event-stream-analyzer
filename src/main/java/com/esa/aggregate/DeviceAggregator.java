package com.esa.aggregate;

import com.esa.model.DeviceEvent;
import com.esa.model.Record;
import com.esa.model.RecordType;
import com.esa.model.Severity;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class DeviceAggregator implements Aggregator
{
    private final Map<Severity, Integer> countsBySeverity = new EnumMap<>(Severity.class);
    private final Map<String, Map<Severity, Integer>> severityCountsByDevice = new LinkedHashMap<>();

    @Override
    public RecordType handles()
    {
        return RecordType.DEVICE;
    }

    @Override
    public void accept(Record record)
    {
        DeviceEvent event = (DeviceEvent) record;
        countsBySeverity.merge(event.severity(), 1, Integer::sum);
        severityCountsByDevice
                .computeIfAbsent(event.device(), key -> new EnumMap<>(Severity.class))
                .merge(event.severity(), 1, Integer::sum);
    }

    @Override
    public Map<String, Object> result()
    {
        if (countsBySeverity.isEmpty())
        {
            return Map.of();
        }

        Map<String, Object> bySeverity = new LinkedHashMap<>();
        for (Map.Entry<Severity, Integer> entry : countsBySeverity.entrySet())
        {
            bySeverity.put(entry.getKey().name(), entry.getValue());
        }

        Map<String, Object> byDevice = new LinkedHashMap<>();
        for (Map.Entry<String, Map<Severity, Integer>> entry : severityCountsByDevice.entrySet())
        {
            byDevice.put(entry.getKey(), summarizeDevice(entry.getValue()));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("by_severity", bySeverity);
        result.put("by_device", byDevice);
        return result;
    }

    private static Map<String, Object> summarizeDevice(Map<Severity, Integer> severityCounts)
    {
        int total = 0;
        for (int count : severityCounts.values())
        {
            total += count;
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", total);
        summary.put("most_frequent_severity", mostFrequentSeverity(severityCounts).name());
        return summary;
    }

    // Highest count wins; ties go to the highest severity, which is the lowest
    // enum ordinal since Severity is declared CRITICAL first (FR-14).
    private static Severity mostFrequentSeverity(Map<Severity, Integer> severityCounts)
    {
        Severity winner = null;
        int winnerCount = -1;
        for (Map.Entry<Severity, Integer> entry : severityCounts.entrySet())
        {
            Severity severity = entry.getKey();
            int count = entry.getValue();
            if (winner != null && !winsOver(severity, count, winner, winnerCount))
            {
                continue;
            }
            winner = severity;
            winnerCount = count;
        }
        return winner;
    }

    private static boolean winsOver(Severity severity, int count, Severity best, int bestCount)
    {
        if (count != bestCount)
        {
            return count > bestCount;
        }
        return severity.compareTo(best) < 0;
    }
}
