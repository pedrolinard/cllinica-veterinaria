package com.vetclinic.api.common.exception;

public class InvalidCurrentPasswordException extends RuntimeException {

    public InvalidCurrentPasswordException(String message) {
        super(message);
    }
}
