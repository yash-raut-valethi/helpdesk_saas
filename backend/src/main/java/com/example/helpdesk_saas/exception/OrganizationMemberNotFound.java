package com.example.helpdesk_saas.exception;

public class OrganizationMemberNotFound extends RuntimeException {
    public OrganizationMemberNotFound(String message) {
        super(message);
    }
}
