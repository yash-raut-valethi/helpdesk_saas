package com.example.helpdesk_saas.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import lombok.Data;

@Data
public class OrganizationUpdateRequestDTO {

    private String name;

    private String description;

    @Email(message = "Contact email is invalid")
    private String contactEmail;

    private String contactPhone;

    private String websiteUrl;

    private String address;

    private String logoUrl;

    @NotBlank
    private String slug;
}
