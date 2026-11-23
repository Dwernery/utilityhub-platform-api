package com.utilityhub.api.dto.json;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/**
 * Reads an Integer that must be written as a whole number. By default Jackson quietly turns
 * {@code 2.5} into {@code 2}; with this a decimal (or a string) is a 400, like any other bad input.
 * Use it with {@code @JsonDeserialize(using = WholeNumberDeserializer.class)} on one field.
 */
public class WholeNumberDeserializer extends ValueDeserializer<Integer> {

    @Override
    public Integer deserialize(JsonParser p, DeserializationContext ctxt) {
        if (p.currentToken() != JsonToken.VALUE_NUMBER_INT) {
            return (Integer) ctxt.handleUnexpectedToken(Integer.class, p);
        }
        return p.getIntValue();
    }
}
