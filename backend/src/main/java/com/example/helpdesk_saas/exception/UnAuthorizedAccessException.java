package com.example.helpdesk_saas.exception;

public class UnAuthorizedAccessException extends RuntimeException {
  public UnAuthorizedAccessException(String message) {
    super(message);
  }
}
