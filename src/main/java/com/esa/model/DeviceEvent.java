package com.esa.model;

public record DeviceEvent(String device, Severity severity) implements Record
{
    @Override
    public RecordType type()
    {
        return RecordType.DEVICE;
    }
}
