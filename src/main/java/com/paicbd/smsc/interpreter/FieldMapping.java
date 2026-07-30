package com.paicbd.smsc.interpreter;

public record FieldMapping(
        String sourceProperty,
        String targetProperty,
        DataType dataType
) {
}