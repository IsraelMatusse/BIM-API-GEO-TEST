package com.bim.api_test.infrastructure.exceptions;

public class ConflictException extends Exception {
    public ConflictException(String message){
        super(message);
    }
}
