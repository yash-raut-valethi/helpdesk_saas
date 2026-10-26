package com.example.helpdesk_saas.dto;


import lombok.Data;

@Data
public class LoginResponse {
    private String jwtToken;
    private String refreshToken;
}
