package com.example.helpdesk_saas.dto;


import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class AdminTransferRequestDTO {
    @Email(message= "User email is invalid")
    private String currentUserEmail;
    private String slug;
}
