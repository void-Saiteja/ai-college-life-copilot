package com.collegecopilot.career;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@Converter
public class CareerObjectListJsonConverter implements AttributeConverter<List<Object>, String> {

    private static final Logger log = LoggerFactory.getLogger(CareerObjectListJsonConverter.class);
    private static final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Override
    public String convertToDatabaseColumn(List<Object> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (Exception e) {
            log.error("Error serializing List<Object> to JSON: {}", e.getMessage());
            return "[]";
        }
    }

    @Override
    public List<Object> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(dbData, new TypeReference<List<Object>>() {});
        } catch (Exception e) {
            log.warn("Error deserializing JSON to List<Object>: {}", dbData);
            return new ArrayList<>();
        }
    }
}
