package com.esa.aggregate;

import com.esa.model.AccessRecord;
import com.esa.model.Record;
import com.esa.model.RecordType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AccessAggregator implements Aggregator
{
    private static final int DENIAL_FLAG_THRESHOLD = 3;

    private final Map<String, DoorStats> statsByDoor = new LinkedHashMap<>();

    @Override
    public RecordType handles()
    {
        return RecordType.ACCESS;
    }

    @Override
    public void accept(Record record)
    {
        AccessRecord access = (AccessRecord) record;
        statsByDoor
                .computeIfAbsent(access.door(), key -> new DoorStats())
                .add(access);
    }

    @Override
    public Map<String, Object> result()
    {
        Map<String, Object> byDoor = new LinkedHashMap<>();
        for (Map.Entry<String, DoorStats> entry : statsByDoor.entrySet())
        {
            byDoor.put(entry.getKey(), entry.getValue().summarize());
        }
        return byDoor;
    }

    private static final class DoorStats
    {
        private final List<Integer> durations = new ArrayList<>();
        private int granted = 0;
        private int denied = 0;
        private final Map<String, Integer> denialsByUser = new LinkedHashMap<>();

        private void add(AccessRecord access)
        {
            durations.add(access.durationMs());
            if (access.granted())
            {
                granted++;
                return;
            }
            denied++;
            denialsByUser.merge(access.user(), 1, Integer::sum);
        }

        private Map<String, Object> summarize()
        {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("decision_times", decisionTimes());
            summary.put("results", results());
            summary.put("flagged_users", flaggedUsers());
            return summary;
        }

        private Map<String, Object> decisionTimes()
        {
            Map<String, Object> times = new LinkedHashMap<>();
            times.put("min", Collections.min(durations));
            times.put("50_percentile", Stats.nearestRankPercentile(durations, 50));
            times.put("90_percentile", Stats.nearestRankPercentile(durations, 90));
            times.put("99_percentile", Stats.nearestRankPercentile(durations, 99));
            times.put("max", Collections.max(durations));
            return times;
        }

        private Map<String, Object> results()
        {
            Map<String, Object> counts = new LinkedHashMap<>();
            counts.put("GRANTED", granted);
            counts.put("DENIED", denied);
            return counts;
        }

        private List<String> flaggedUsers()
        {
            List<String> flagged = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : denialsByUser.entrySet())
            {
                if (entry.getValue() >= DENIAL_FLAG_THRESHOLD)
                {
                    flagged.add(entry.getKey());
                }
            }
            return flagged;
        }
    }
}
