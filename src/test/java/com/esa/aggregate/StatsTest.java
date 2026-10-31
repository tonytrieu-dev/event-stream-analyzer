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
    void populationStandardDeviationDividesByNNotNMinusOne()
    {
        // Population (/N) = 2.0; sample (/N-1) = ~2.14, so a /N-1 bug fails here (FR-10).
        double stdDev = Stats.populationStdDev(List.of(2.0, 4.0, 4.0, 4.0, 5.0, 5.0, 7.0, 9.0));

        assertEquals(2.0, stdDev, EXACT, "std dev must divide by N, not N-1 (FR-10)");
    }
}
