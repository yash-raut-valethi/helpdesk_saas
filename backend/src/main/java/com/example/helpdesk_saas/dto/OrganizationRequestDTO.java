package com.example.helpdesk_saas.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OrganizationRequestDTO {

    @NotBlank
    private String name;

    @NotBlank
    private String slug;

    private String description;

    private String contactEmail;

    private String contactPhone;

    private String websiteUrl;

    private String address;

    private String logoUrl;

    private String creatorEmail;

}
