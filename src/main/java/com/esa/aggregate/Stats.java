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
        return sum / values.size();
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
        return (sorted.get(middle - 1) + sorted.get(middle)) / 2.0;
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
        return Math.sqrt(sumOfSquaredDeviations / values.size());
    }
}
