package com.example.helpdesk_saas.util;

import com.example.helpdesk_saas.dto.OrganizationResponseDTO;
import com.example.helpdesk_saas.models.Organization;

public class EntityDTOMapper {

    public static OrganizationResponseDTO toOrganizationResponseDTO(Organization organization){

        OrganizationResponseDTO responseDTO = new OrganizationResponseDTO();

        responseDTO.setName(organization.getName());
        responseDTO.setSlug(organization.getSlug());
        responseDTO.setDescription(organization.getDescription());
        responseDTO.setContactEmail(organization.getContactEmail());
        responseDTO.setContactPhone(organization.getContactPhone());
        responseDTO.setWebsiteUrl(organization.getWebsiteUrl());
        responseDTO.setAddress(organization.getAddress());
        responseDTO.setLogoUrl(organization.getLogoUrl());
        responseDTO.setCreatorEmail(organization.getCreatedBy().getEmail());

        return responseDTO;
    }

}
