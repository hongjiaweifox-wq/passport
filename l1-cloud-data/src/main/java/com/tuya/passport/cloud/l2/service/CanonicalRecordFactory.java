package com.tuya.passport.cloud.l2.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

@Component
public class CanonicalRecordFactory {
    private final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, false);

    public ObjectMapper mapper() {
        return mapper;
    }
}
