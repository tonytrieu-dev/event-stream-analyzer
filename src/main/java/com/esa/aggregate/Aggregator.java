package com.esa.aggregate;

import com.esa.model.Record;
import com.esa.model.RecordType;
import java.util.Map;

public interface Aggregator
{
    RecordType handles();

    void accept(Record record);

    Map<String, Object> result();
}
