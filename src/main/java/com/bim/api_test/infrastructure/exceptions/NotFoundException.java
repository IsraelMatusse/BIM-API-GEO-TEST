package com.bim.api_test.infrastructure.exceptions;

public class NotFoundException extends Exception {
    public NotFoundException(String message){
        super(message);
    }
}
