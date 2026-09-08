package com.bim.api_test.infrastructure.exceptions;

public class InternalServerErrorException extends Exception {
    public InternalServerErrorException(String message){
        super(message);
    }
}
