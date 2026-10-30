package com.esa.classify;

import com.esa.model.AccessRecord;
import com.esa.model.DeviceEvent;
import com.esa.model.Record;
import com.esa.model.SensorReading;
import com.esa.model.Severity;
import java.util.Map;
import java.util.Optional;

public class RecordFactory
{
    public Optional<Record> classify(Map<String, String> fields)
    {
        return trySensorReading(fields)
                .or(() -> tryDeviceEvent(fields))
                .or(() -> tryAccessRecord(fields));
    }

    private static Optional<Record> trySensorReading(Map<String, String> fields)
    {
        String sensor = fields.get("sensor");
        String reading = fields.get("reading");
        if (sensor == null || reading == null)
        {
            return Optional.empty();
        }
        Optional<Double> value = parseDouble(reading);
        if (value.isEmpty())
        {
            return Optional.empty();
        }
        return Optional.of(new SensorReading(sensor, value.get()));
    }

    private static Optional<Record> tryDeviceEvent(Map<String, String> fields)
    {
        String severityText = fields.get("severity");
        String device = fields.get("device");
        if (severityText == null || device == null)
        {
            return Optional.empty();
        }
        Optional<Severity> severity = parseSeverity(severityText);
        if (severity.isEmpty())
        {
            return Optional.empty();
        }
        return Optional.of(new DeviceEvent(device, severity.get()));
    }

    private static Optional<Record> tryAccessRecord(Map<String, String> fields)
    {
        String user = fields.get("user");
        String door = fields.get("door");
        String result = fields.get("result");
        String durationText = fields.get("duration_ms");
        if (user == null || door == null || result == null || durationText == null)
        {
            return Optional.empty();
        }
        if (!result.equals("GRANTED") && !result.equals("DENIED"))
        {
            return Optional.empty();
        }
        Optional<Integer> duration = parseInt(durationText);
        if (duration.isEmpty())
        {
            return Optional.empty();
        }
        boolean granted = result.equals("GRANTED");
        return Optional.of(new AccessRecord(user, door, granted, duration.get()));
    }

    private static Optional<Severity> parseSeverity(String text)
    {
        try
        {
            return Optional.of(Severity.valueOf(text));
        }
        catch (IllegalArgumentException notASeverity)
        {
            return Optional.empty();
        }
    }

    private static Optional<Double> parseDouble(String text)
    {
        try
        {
            return Optional.of(Double.parseDouble(text));
        }
        catch (NumberFormatException notANumber)
        {
            return Optional.empty();
        }
    }

    private static Optional<Integer> parseInt(String text)
    {
        try
        {
            return Optional.of(Integer.parseInt(text));
        }
        catch (NumberFormatException notAnInteger)
        {
            return Optional.empty();
        }
    }
}
