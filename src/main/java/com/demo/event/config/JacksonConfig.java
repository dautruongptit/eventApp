package com.demo.event.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JacksonConfig {

    /**
     * Khai bao ObjectMapper la @Bean @Primary de:
     * 1. Fix loi "Could not autowire. No beans of ObjectMapper type found"
     *    khi inject ObjectMapper vao RelativeService bang @RequiredArgsConstructor.
     * 2. Cau hinh toan cuc: serialize LocalDate/LocalDateTime dung ISO-8601,
     *    khong throw loi khi gap field la, v.v.
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Ho tro serialize/deserialize Java 8 Date/Time API
        // (LocalDate, LocalDateTime, LocalTime)
        mapper.registerModule(new JavaTimeModule());

        // Serialize LocalDate thanh "2026-05-15" thay vi array [2026, 5, 15]
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Khong throw loi khi JSON co field ma POJO khong co
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        // Khong throw loi khi tat ca properties cua POJO deu null
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        return mapper;
    }
}
