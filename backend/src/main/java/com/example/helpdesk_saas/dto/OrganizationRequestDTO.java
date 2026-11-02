package com.example.helpdesk_saas.dto;

import lombok.Data;

@Data
public class OrganizationRequestDTO {

    private String name;

    private String slug;

    private String description;

    private String contactEmail;

    private String contactPhone;

    private String websiteUrl;

    private String address;

    private String logoUrl;

    private String creatorEmail;

}
