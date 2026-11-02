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

        assertEquals(Map.of("severity", "INFO", "msg", "Heartbeat a=b OK"), parsedFields);
    }

    @Test
    void rejectsTheWholeLineWhenTokensOrQuotesAreMalformed()
    {
        assertEquals(Map.of(), parser.parse("sensor=temp reading=20 msg=\"broken"));
        for (String malformed : new String[] {
                "ts=t sensor=temp reading=20 ###CORRUPTED LINE### !!!",
                "ts=t sensor=\"air quality\"suffix reading=20",
                "sensor=prefix\"air quality\" reading=20",
                "sensor=\"air\"\"quality\" reading=20",
                "\"sensor\"=temp reading=20",
                "sensor=temp reading=20 =missing-key"})
        {
            assertEquals(Map.of(), parser.parse(malformed), malformed);
        }
    }
}
