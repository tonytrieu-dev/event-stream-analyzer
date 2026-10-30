package com.esa.model;

public record SensorReading(String sensor, double reading) implements Record
{
    @Override
    public RecordType type()
    {
        return RecordType.SENSOR;
    }
}
