package com.example.helpdesk_saas.exception;

public class OrganizationAlreadyExistException extends RuntimeException {
    public OrganizationAlreadyExistException(String message) {
        super(message);
    }
}
