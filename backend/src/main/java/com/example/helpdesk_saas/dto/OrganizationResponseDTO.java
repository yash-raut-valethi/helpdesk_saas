package com.example.helpdesk_saas.dto;


import com.example.helpdesk_saas.models.User;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class OrganizationResponseDTO {

    private String name;

    private String slug;

    private String description;

    private String contactEmail;

    private String contactPhone;

    private String websiteUrl;

    private String logoUrl;

    private String address;

    private String creatorEmail;

    private String admin;

}
