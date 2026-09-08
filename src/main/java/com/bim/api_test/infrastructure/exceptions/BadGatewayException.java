package com.bim.api_test.infrastructure.exceptions;


public class BadGatewayException extends Exception {
    public BadGatewayException(String message) {
        super(message);
    }
}
