package com.collegecopilot.career;

import com.collegecopilot.career.dto.InterviewScoresDto;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Converter
public class InterviewScoresJsonConverter implements AttributeConverter<InterviewScoresDto, String> {

    private static final Logger log = LoggerFactory.getLogger(InterviewScoresJsonConverter.class);
    private static final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Override
    public String convertToDatabaseColumn(InterviewScoresDto attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (Exception e) {
            log.error("Error serializing InterviewScoresDto to JSON: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public InterviewScoresDto convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(dbData, InterviewScoresDto.class);
        } catch (Exception e) {
            log.warn("Error deserializing JSON to InterviewScoresDto: {}", dbData);
            return null;
        }
    }
}
