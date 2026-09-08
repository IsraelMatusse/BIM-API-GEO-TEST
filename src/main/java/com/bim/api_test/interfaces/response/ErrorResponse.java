package com.bim.api_test.interfaces.response;


public record ErrorResponse(
        String message,
        String correlationId
) {
}
