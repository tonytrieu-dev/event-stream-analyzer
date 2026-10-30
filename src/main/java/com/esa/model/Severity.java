package com.esa.model;

// Order matters: highest first, so natural enum order breaks severity ties (FR-14).
public enum Severity
{
    CRITICAL,
    ERROR,
    WARN,
    INFO
}
