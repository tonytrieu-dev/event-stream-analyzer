package com.esa.aggregate;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class StatsTest
{
    private static final double EXACT = 0.0;

    @Test
    void medianOfAnEvenCountAveragesTheTwoMiddleValues()
    {
        assertEquals(25.0, Stats.median(List.of(10.0, 20.0, 30.0, 40.0)), EXACT,
                "even N median must be the average of the two middle values (FR-11)");
    }

    @Test
    void medianOfAnOddCountIsTheSingleMiddleValueAndDoesNotAssumeSortedInput()
    {
        assertEquals(20.0, Stats.median(List.of(60.0, 10.0, 20.0)), EXACT,
                "odd N median is the single middle value, computed on sorted order (FR-11)");
    }

    @Test
    void populationStandardDeviationDividesByNNotNMinusOne()
    {
        // Population (/N) = 2.0; sample (/N-1) = ~2.14, so a /N-1 bug fails here (FR-10).
        double stdDev = Stats.populationStdDev(List.of(2.0, 4.0, 4.0, 4.0, 5.0, 5.0, 7.0, 9.0));

        assertEquals(2.0, stdDev, EXACT, "std dev must divide by N, not N-1 (FR-10)");
    }

    @Test
    void meanIsTheArithmeticAverage()
    {
        assertEquals(5.0, Stats.mean(List.of(2.0, 4.0, 4.0, 4.0, 5.0, 5.0, 7.0, 9.0)), EXACT);
    }

    @Test
    void nearestRankPercentileUsesCeilIndexAndDiffersFromTheMedian()
    {
        // p50 nearest-rank = 28, but the median would be 29: they must differ (FR-18).
        List<Integer> durations = List.of(12, 20, 28, 30, 35, 60);

        assertEquals(28, Stats.nearestRankPercentile(durations, 50),
                "nearest-rank p50 is sorted[ceil(p/100*N)-1], not the median (FR-18)");
        assertEquals(60, Stats.nearestRankPercentile(durations, 90));
        assertEquals(60, Stats.nearestRankPercentile(durations, 99));
    }
}
