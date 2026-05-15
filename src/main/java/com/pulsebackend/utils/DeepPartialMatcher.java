package com.pulsebackend.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.Collection;
import java.util.Map;

public final class DeepPartialMatcher {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private DeepPartialMatcher() {
    }

    @SuppressWarnings("unchecked")
    public static <T> boolean matches(T candidate, T partial) {
        if (partial == null) {
            return true;
        }
        if (candidate == null) {
            return false;
        }

        Map<String, Object> candidateMap = MAPPER.convertValue(candidate, Map.class);
        Map<String, Object> partialMap = MAPPER.convertValue(partial, Map.class);
        return mapMatches(candidateMap, partialMap);
    }

    private static boolean mapMatches(Map<String, Object> candidate, Map<String, Object> partial) {
        for (Map.Entry<String, Object> entry : partial.entrySet()) {
            Object partialValue = entry.getValue();
            if (partialValue == null) {
                continue;
            }

            Object candidateValue = candidate.get(entry.getKey());
            if (!valueMatches(candidateValue, partialValue)) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private static boolean valueMatches(Object candidateValue, Object partialValue) {
        if (partialValue == null) {
            return true;
        }
        if (candidateValue == null) {
            return false;
        }
        if (isScalar(partialValue)) {
            return partialValue.equals(candidateValue);
        }
        if (partialValue instanceof Map<?, ?> && candidateValue instanceof Map<?, ?>) {
            return mapMatches((Map<String, Object>) candidateValue, (Map<String, Object>) partialValue);
        }
        if (partialValue instanceof Collection<?> partialCollection
                && candidateValue instanceof Collection<?> candidateCollection) {
            return candidateCollection.containsAll(partialCollection);
        }
        return partialValue.equals(candidateValue);
    }

    private static boolean isScalar(Object value) {
        return value instanceof String
                || value instanceof Number
                || value instanceof Boolean
                || value.getClass().isEnum();
    }
}
