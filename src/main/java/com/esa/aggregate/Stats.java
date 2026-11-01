package com.esa.aggregate;

import java.util.ArrayList;
import java.util.List;

public final class Stats
{
    private Stats()
    {
    }

    public static double mean(List<Double> values)
    {
        double sum = 0.0;
        for (double value : values)
        {
            sum += value;
        }
        if (Double.isFinite(sum))
        {
            return sum / values.size();
        }

        // Normalize only when the intermediate sum exceeds the double range.
        double scale = maxMagnitude(values);
        double normalizedSum = 0.0;
        for (double value : values)
        {
            normalizedSum += value / scale;
        }
        double normalizedMean = normalizedSum / values.size();
        return Math.max(-1.0, Math.min(1.0, normalizedMean)) * scale;
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
        double lower = sorted.get(middle - 1);
        double upper = sorted.get(middle);
        double sum = lower + upper;
        return Double.isFinite(sum) ? sum / 2.0 : lower / 2.0 + upper / 2.0;
    }

    public static double populationStdDev(List<Double> values)
    {
        double mean = mean(values);
        double sumOfSquaredDeviations = 0.0;
        for (double value : values)
        {
            double deviation = value - mean;
            sumOfSquaredDeviations += deviation * deviation;
        }
        if (Double.isFinite(sumOfSquaredDeviations))
        {
            return Math.sqrt(sumOfSquaredDeviations / values.size());
        }

        double scale = maxMagnitude(values);
        double normalizedMean = mean / scale;
        double normalizedSum = 0.0;
        for (double value : values)
        {
            double deviation = value / scale - normalizedMean;
            normalizedSum += deviation * deviation;
        }
        // Population standard deviation cannot exceed the largest absolute input.
        return Math.sqrt(Math.min(1.0, normalizedSum / values.size())) * scale;
    }

    private static double maxMagnitude(List<Double> values)
    {
        double maximum = 0.0;
        for (double value : values)
        {
            maximum = Math.max(maximum, Math.abs(value));
        }
        return maximum;
    }

    // Nearest-rank, deliberately not the median for even N (FR-18).
    public static int nearestRankPercentile(List<Integer> values, int percentile)
    {
        List<Integer> sorted = new ArrayList<>(values);
        sorted.sort(null);

        int rank = (int) Math.ceil(percentile / 100.0 * sorted.size());
        int index = rank - 1;
        return sorted.get(index);
    }
}
