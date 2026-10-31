package com.esa.aggregate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.esa.model.AccessRecord;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AccessAggregatorTest
{
    private final AccessAggregator aggregator = new AccessAggregator();

    private void accept(String user, String door, boolean granted, int durationMs)
    {
        aggregator.accept(new AccessRecord(user, door, granted, durationMs));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> door(String name)
    {
        return (Map<String, Object>) aggregator.result().get(name);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> decisionTimes(String doorName)
    {
        return (Map<String, Object>) door(doorName).get("decision_times");
    }

    @Test
    void reportsMinimumAndMaximumDurationPerDoor()
    {
        accept("u1", "door-A", true, 30);
        accept("u2", "door-A", true, 12);
        accept("u3", "door-A", false, 60);

        assertEquals(12, decisionTimes("door-A").get("min"));
        assertEquals(60, decisionTimes("door-A").get("max"));
    }

    @Test
    void usesNearestRankPercentilesWhichDifferFromTheMedian()
    {
        // N=10: p50=35, p90=55, p99=60 are all distinct (so a swapped percentile
        // key is visible), and p50 (35) differs from the median (37.5) (FR-18).
        for (int duration : new int[] {10, 20, 28, 30, 35, 40, 45, 50, 55, 60})
        {
            accept("u", "door-A", true, duration);
        }

        assertEquals(35, decisionTimes("door-A").get("50_percentile"),
                "p50 must be nearest-rank (35), not the median (37.5) (FR-18)");
        assertEquals(55, decisionTimes("door-A").get("90_percentile"));
        assertEquals(60, decisionTimes("door-A").get("99_percentile"));
    }

    @Test
    void countsGrantedVersusDeniedPerDoor()
    {
        accept("u1", "door-A", true, 10);
        accept("u2", "door-A", true, 10);
        accept("u3", "door-A", false, 10);

        @SuppressWarnings("unchecked")
        Map<String, Object> results = (Map<String, Object>) door("door-A").get("results");
        assertEquals(2, results.get("GRANTED"));
        assertEquals(1, results.get("DENIED"));
    }

    @Test
    void flagsAUserOnlyAtThreeOrMoreDenialsOnTheSameDoor()
    {
        accept("twice", "door-A", false, 10);
        accept("twice", "door-A", false, 10);
        accept("thrice", "door-A", false, 10);
        accept("thrice", "door-A", false, 10);
        accept("thrice", "door-A", false, 10);

        @SuppressWarnings("unchecked")
        List<String> flagged = (List<String>) door("door-A").get("flagged_users");
        assertTrue(flagged.contains("thrice"), "3 denials must flag the user (FR-17)");
        assertFalse(flagged.contains("twice"), "2 denials must NOT flag the user (boundary)");
    }
}
