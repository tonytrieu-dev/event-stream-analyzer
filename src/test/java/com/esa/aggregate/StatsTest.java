package com.esa.aggregate;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.IntStream;
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
    void nearestRankUsesTheExactBoundaryAndRoundsUpOnlyAboveIt()
    {
        for (int size : new int[] {99, 100, 101})
        {
            List<Integer> sorted = IntStream.rangeClosed(1, size).boxed().toList();
            int expected = size == 101 ? 8 : 7;

            assertEquals(expected, Stats.nearestRankPercentile(sorted, 7),
                    "p7 of 1.." + size + ": floating-point error must not advance an exact rank");
        }
    }

    @Test
    void keepsRepresentableStatisticsFiniteForLargeReadings()
    {
        for (double magnitude : new double[] {1e155, 1e308, Double.MAX_VALUE})
        {
            List<Double> opposite = List.of(magnitude, -magnitude);
            assertEquals(0.0, Stats.mean(opposite));
            assertEquals(0.0, Stats.median(opposite));
            assertEquals(magnitude, Stats.populationStdDev(opposite), magnitude * 1e-14);

            List<Double> identical = List.of(magnitude, magnitude);
            assertEquals(magnitude, Stats.mean(identical));
            assertEquals(magnitude, Stats.median(identical));
            assertEquals(0.0, Stats.populationStdDev(identical));
        }
    }
}
