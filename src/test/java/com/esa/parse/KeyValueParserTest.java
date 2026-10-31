package com.esa.parse;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class KeyValueParserTest
{
    private final LineParser parser = new KeyValueParser();

    @Test
    void keepsSpacesInsideAQuotedValueAndExtractsEveryPairExactly()
    {
        Map<String, String> parsedFields = parser.parse("severity=INFO msg=\"Heartbeat OK\"");

        // Full-map assertion: every pair is extracted, nothing spurious is added,
        // and the quoted value keeps its space as one value.
        assertEquals(Map.of("severity", "INFO", "msg", "Heartbeat OK"), parsedFields);
    }

    @Test
    void doesNotSplitOnAnEqualsSignInsideAQuotedValue()
    {
        Map<String, String> parsedFields = parser.parse("msg=\"a=b\"");

        assertEquals("a=b", parsedFields.get("msg"),
                "split must happen on the first = only, not on = inside a quoted value");
    }
}
