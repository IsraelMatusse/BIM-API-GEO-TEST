package com.bim.api_test.interfaces.dtos.response;


public record ErrorResponse(
        String message,
        String correlationId
) {
}
