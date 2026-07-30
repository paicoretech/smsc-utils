package com.paicbd.smsc.interpreter;

import java.util.Map;

public record ParsedSpec(
        String propertyName,
        DataType propertyType,
        String elementName,
        Map<String,String> renameMap
) {}
