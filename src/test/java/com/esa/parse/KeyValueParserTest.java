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
        Map<String, String> parsedFields = parser.parse("severity=INFO msg=\"Heartbeat a=b OK\"");

        // Full-map assertion: every pair is extracted, nothing spurious is added,
        // and the quoted value keeps its space as one value.
        assertEquals(Map.of("severity", "INFO", "msg", "Heartbeat a=b OK"), parsedFields);
    }

    @Test
    void rejectsTheWholeLineWhenAQuotedValueIsUnfinished()
    {
        assertEquals(Map.of(), parser.parse("severity=INFO device=\"broken lock"));
        assertEquals(Map.of(), parser.parse("sensor=temp reading=20 msg=\"broken"));
        assertEquals(Map.of("device", "lock", "severity", "INFO"),
                parser.parse("device=lock severity=INFO"));
    }
}
