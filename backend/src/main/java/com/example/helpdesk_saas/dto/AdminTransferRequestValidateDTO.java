package com.example.helpdesk_saas.dto;


import lombok.Data;

@Data
public class AdminTransferRequestValidateDTO {
    private String otp;
    private String currentUserEmail;
}
