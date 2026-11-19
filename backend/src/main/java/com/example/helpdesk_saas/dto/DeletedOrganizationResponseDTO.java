package com.example.helpdesk_saas.dto;

import lombok.Data;

@Data
public class DeletedOrganizationResponseDTO {

    private String name;

    private String slug;

    private String description;

    private String contactEmail;

    private String contactPhone;

}
