package com.example.learnservice.util;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ValidateUtil {

    public static String validateKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }

        String trimmed = keyword.trim();

        return trimmed.toLowerCase();
    }

    /**
     * Clean và validate position IDs
     */
    public static List<Long> cleanPositionIds(List<Long> positionIds) {
        if (positionIds == null || positionIds.isEmpty()) {
            return null;
        }

        // Remove null và invalid IDs
        positionIds.removeIf(id -> id == null || id <= 0);

        return positionIds.isEmpty() ? null : positionIds;
    }

    public static List<String> validateSearchFields(List<String> searchFields) {
        if (searchFields == null || searchFields.isEmpty()) {
            return null;
        }

        Set<String> allowedFields = Set.of(
                "name", "documentNumber", "tags");

        return searchFields.stream()
                .filter(field -> field != null && allowedFields.contains(field.trim()))
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
    }

    public static List<String> validateSearchFieldsForSemester(List<String> searchFields) {
        if (searchFields == null || searchFields.isEmpty()) {
            return null;
        }

        Set<String> allowedFields = Set.of(
                "name");

        return searchFields.stream()
                .filter(field -> field != null && allowedFields.contains(field.trim()))
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
    }

}
