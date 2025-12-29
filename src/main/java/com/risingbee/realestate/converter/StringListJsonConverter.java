package com.risingbee.realestate.converter;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Converter(autoApply = false)
public class StringListJsonConverter
        implements AttributeConverter<List<String>, String> {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        try {
            return mapper.writeValueAsString(
                attribute == null ? List.of() : attribute
            );
        } catch (Exception e) {
            throw new IllegalStateException("JSON write failed", e);
        }
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        try {
        	return (dbData == null || dbData.isBlank())
        		    ? new ArrayList<>()
        		    : mapper.readValue(dbData, new TypeReference<>() {});

        } catch (Exception e) {
            throw new IllegalStateException("JSON read failed", e);
        }
    }
}
