package com.esa.parse;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class KeyValueParser implements LineParser
{
    @Override
    public Map<String, String> parse(String line)
    {
        Map<String, String> fields = new LinkedHashMap<>();
        for (String token : tokenize(line))
        {
            int firstEqualsIndex = token.indexOf('=');
            if (firstEqualsIndex <= 0)
            {
                continue;
            }
            String key = token.substring(0, firstEqualsIndex);
            String value = unquote(token.substring(firstEqualsIndex + 1));
            fields.put(key, value);
        }
        return fields;
    }

    private static List<String> tokenize(String line)
    {
        List<String> tokens = new ArrayList<>();
        StringBuilder currentToken = new StringBuilder();
        boolean inQuotes = false;

        for (int position = 0; position < line.length(); position++)
        {
            char character = line.charAt(position);

            if (character == '"')
            {
                inQuotes = !inQuotes;
                currentToken.append(character);
                continue;
            }
            if (character == ' ' && !inQuotes)
            {
                flush(tokens, currentToken);
                continue;
            }
            currentToken.append(character);
        }

        flush(tokens, currentToken);
        return tokens;
    }

    private static void flush(List<String> tokens, StringBuilder currentToken)
    {
        if (currentToken.length() == 0)
        {
            return;
        }
        tokens.add(currentToken.toString());
        currentToken.setLength(0);
    }

    private static String unquote(String value)
    {
        boolean quoted = value.length() >= 2
                && value.startsWith("\"")
                && value.endsWith("\"");
        if (!quoted)
        {
            return value;
        }
        return value.substring(1, value.length() - 1);
    }
}
