package com.esa.aggregate;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    void usesNearestRankPercentilesWhichDifferFromTheMedian()
    {
        for (int duration : new int[] {60, 10, 45, 28, 55, 20, 40, 50, 30, 35})
        {
            accept("u", "door-A", true, duration);
        }

        accept("u", "door-B", true, 99);

        assertEquals(10, decisionTimes("door-A").get("min"));
        assertEquals(60, decisionTimes("door-A").get("max"));
        assertEquals(35, decisionTimes("door-A").get("50_percentile"),
                "p50 must be nearest-rank (35), not the median (37.5) (FR-18)");
        assertEquals(55, decisionTimes("door-A").get("90_percentile"));
        assertEquals(60, decisionTimes("door-A").get("99_percentile"));
        assertEquals(Map.of("min", 99, "50_percentile", 99, "90_percentile", 99,
                "99_percentile", 99, "max", 99), decisionTimes("door-B"));
    }

    @Test
    void countsGrantedVersusDeniedPerDoor()
    {
        accept("u1", "door-A", true, 10);
        accept("u2", "door-A", true, 10);
        accept("u3", "door-A", false, 10);
        accept("u1", "door-B", false, 10);

        @SuppressWarnings("unchecked")
        Map<String, Object> results = (Map<String, Object>) door("door-A").get("results");
        assertEquals(2, results.get("GRANTED"));
        assertEquals(1, results.get("DENIED"));
        assertEquals(Map.of("GRANTED", 0, "DENIED", 1), door("door-B").get("results"));
    }

    @Test
    void flagsAUserOnlyAtThreeOrMoreDenialsOnTheSameDoor()
    {
        accept("twice", "door-A", false, 10);
        accept("twice", "door-A", false, 10);
        accept("thrice", "door-A", false, 10);
        accept("thrice", "door-A", false, 10);
        accept("thrice", "door-A", false, 10);

        accept("twice", "door-B", false, 10);
        accept("thrice", "door-B", false, 10);
        accept("granted-only", "door-A", true, 10);
        accept("granted-only", "door-A", true, 10);
        accept("granted-only", "door-A", true, 10);

        assertEquals(List.of("thrice"), door("door-A").get("flagged_users"));
        assertEquals(List.of(), door("door-B").get("flagged_users"));
    }
}
