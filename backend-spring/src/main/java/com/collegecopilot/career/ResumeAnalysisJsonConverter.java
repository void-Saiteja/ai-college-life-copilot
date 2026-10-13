package com.collegecopilot.career;

import com.collegecopilot.career.dto.ResumeAnalysisDto;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Converter
public class ResumeAnalysisJsonConverter implements AttributeConverter<ResumeAnalysisDto, String> {

    private static final Logger log = LoggerFactory.getLogger(ResumeAnalysisJsonConverter.class);
    private static final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Override
    public String convertToDatabaseColumn(ResumeAnalysisDto attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (Exception e) {
            log.error("Error serializing ResumeAnalysisDto to JSON: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public ResumeAnalysisDto convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(dbData, ResumeAnalysisDto.class);
        } catch (Exception e) {
            log.warn("Error deserializing JSON to ResumeAnalysisDto: {}", dbData);
            return null;
        }
    }
}
