package com.esa.aggregate;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;

public final class Stats
{
    private Stats()
    {
    }

    public static double mean(List<Double> values)
    {
        return decimalMean(values).doubleValue();
    }

    private static BigDecimal decimalMean(List<Double> values)
    {
        BigDecimal sum = BigDecimal.ZERO;
        for (double value : values)
        {
            sum = sum.add(BigDecimal.valueOf(value));
        }
        return sum.divide(BigDecimal.valueOf(values.size()), MathContext.DECIMAL128);
    }

    public static double median(List<Double> values)
    {
        List<Double> sorted = new ArrayList<>(values);
        sorted.sort(null);

        int size = sorted.size();
        int middle = size / 2;
        if (size % 2 == 1)
        {
            return sorted.get(middle);
        }
        BigDecimal lower = BigDecimal.valueOf(sorted.get(middle - 1));
        BigDecimal upper = BigDecimal.valueOf(sorted.get(middle));
        return lower.add(upper).divide(BigDecimal.valueOf(2)).doubleValue();
    }

    public static double populationStdDev(List<Double> values)
    {
        BigDecimal mean = decimalMean(values);
        BigDecimal sumOfSquaredDeviations = BigDecimal.ZERO;
        for (double value : values)
        {
            BigDecimal deviation = BigDecimal.valueOf(value).subtract(mean);
            sumOfSquaredDeviations = sumOfSquaredDeviations.add(deviation.multiply(deviation));
        }
        return sumOfSquaredDeviations.divide(BigDecimal.valueOf(values.size()), MathContext.DECIMAL128)
                .sqrt(MathContext.DECIMAL128).doubleValue();
    }

    // Nearest-rank, deliberately not the median for even N (FR-18).
    public static int nearestRankPercentile(List<Integer> sorted, int percentile)
    {
        int rank = (int) Math.ceil(percentile / 100.0 * sorted.size());
        int index = rank - 1;
        return sorted.get(index);
    }
}
