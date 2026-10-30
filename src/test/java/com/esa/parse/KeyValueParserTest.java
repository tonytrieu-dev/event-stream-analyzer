package com.esa.parse;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class KeyValueParserTest
{
    private final LineParser parser = new KeyValueParser();

    @Test
    void parsesSpaceSeparatedPairsIntoKeyValueMap()
    {
        Map<String, String> parsedFields = parser.parse("k=v a=b");

        assertEquals(Map.of("k", "v", "a", "b"), parsedFields);
    }

    @Test
    void keepsSpacesInsideAQuotedValueAsASingleValue()
    {
        Map<String, String> parsedFields = parser.parse("severity=INFO msg=\"Heartbeat OK\"");

        assertEquals("Heartbeat OK", parsedFields.get("msg"),
                "a double-quoted value must keep its spaces as one value");
        assertEquals("INFO", parsedFields.get("severity"));
    }

    @Test
    void doesNotSplitOnAnEqualsSignInsideAQuotedValue()
    {
        Map<String, String> parsedFields = parser.parse("msg=\"a=b\"");

        assertEquals("a=b", parsedFields.get("msg"),
                "split must happen on the first = only, not on = inside a quoted value");
    }
}
