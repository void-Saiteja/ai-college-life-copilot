package com.collegecopilot.career;

import com.collegecopilot.career.dto.InterviewFeedbackDto;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Converter
public class InterviewFeedbackJsonConverter implements AttributeConverter<InterviewFeedbackDto, String> {

    private static final Logger log = LoggerFactory.getLogger(InterviewFeedbackJsonConverter.class);
    private static final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Override
    public String convertToDatabaseColumn(InterviewFeedbackDto attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (Exception e) {
            log.error("Error serializing InterviewFeedbackDto to JSON: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public InterviewFeedbackDto convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(dbData, InterviewFeedbackDto.class);
        } catch (Exception e) {
            log.warn("Error deserializing JSON to InterviewFeedbackDto: {}", dbData);
            return null;
        }
    }
}
