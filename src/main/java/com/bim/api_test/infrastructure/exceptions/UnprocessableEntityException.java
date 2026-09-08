package com.bim.api_test.infrastructure.exceptions;

public class UnprocessableEntityException extends Exception {
    public UnprocessableEntityException(String message){
        super(message);
    }
}
