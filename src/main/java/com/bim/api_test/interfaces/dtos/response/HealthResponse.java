package com.bim.api_test.interfaces.dtos.response;

public record HealthResponse(
        String status,
        String version,
        String databaseStatus
) {
}
