package com.bim.api_test.interfaces.response;

public record HealthResponse(
        String status,
        String version,
        String databaseStatus
) {
}
