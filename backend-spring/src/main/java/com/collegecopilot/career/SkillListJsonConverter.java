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
public class SkillListJsonConverter implements AttributeConverter<List<SkillItem>, String> {

    private static final Logger log = LoggerFactory.getLogger(SkillListJsonConverter.class);
    private static final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Override
    public String convertToDatabaseColumn(List<SkillItem> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (Exception e) {
            log.error("Error serializing List<SkillItem> to JSON: {}", e.getMessage());
            return "[]";
        }
    }

    @Override
    public List<SkillItem> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(dbData, new TypeReference<List<SkillItem>>() {});
        } catch (Exception e) {
            log.warn("Error deserializing JSON to List<SkillItem>: {}", dbData);
            return new ArrayList<>();
        }
    }
}
