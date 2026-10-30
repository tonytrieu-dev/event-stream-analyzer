package com.esa.parse;

import java.util.Map;

public interface LineParser
{
    Map<String, String> parse(String line);
}
