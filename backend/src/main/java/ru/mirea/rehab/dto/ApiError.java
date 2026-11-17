package ru.mirea.rehab.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Единый JSON-формат ответов с ошибками REST API.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String correlationId,
        Map<String, String> fieldErrors
) {
}

