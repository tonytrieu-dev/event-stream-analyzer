package com.esa.model;

public record AccessRecord(String user, String door, boolean granted, int durationMs)
        implements Record
{
    @Override
    public RecordType type()
    {
        return RecordType.ACCESS;
    }
}
