package com.example.financemgmt.exception;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) { super(message); }
}
