package com.paicbd.smsc.utils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.paicbd.smsc.interpreter.FieldMapping;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@Generated
public class InterpreterMapper {
    @JsonProperty("event_type")
    private String eventType;
    private String direction;
    private List<FieldMapping> fields;

    @Override
    public String toString() {
        return Converter.valueAsString(this);
    }
}
