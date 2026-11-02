package com.example.helpdesk_saas.service;

import com.example.helpdesk_saas.dto.OrganizationResponseDTO;
import com.example.helpdesk_saas.exception.OrganizationAlreadyExistException;
import com.example.helpdesk_saas.exception.UserNotFoundException;
import com.example.helpdesk_saas.models.Organization;
import com.example.helpdesk_saas.models.User;
import com.example.helpdesk_saas.repository.OrganizationRepository;
import com.example.helpdesk_saas.repository.UserRepository;
import com.example.helpdesk_saas.util.EntityDTOMapper;
import org.springframework.stereotype.Service;


@Service
public class OrganizationService {

    private OrganizationRepository organizationRepository;
    private UserRepository userRepository;

    public OrganizationService(OrganizationRepository organizationRepository,  UserRepository userRepository) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
    }

    public OrganizationResponseDTO createOrganization(String name, String slug, String description, String contactEmail, String contactPhone, String websiteUrl, String address, String logoUrl, String creatorEmail){

        User creator = userRepository.findByEmail(creatorEmail).orElseThrow(()-> new UserNotFoundException("User is not authorized to perform action"));
        Organization optionalOrganization = organizationRepository.findByName(name).orElseThrow(()-> new OrganizationAlreadyExistException("Organization with this name already exists"));
        Organization optionalOrganization2 = organizationRepository.findBySlug(slug).orElseThrow(()-> new OrganizationAlreadyExistException("Organization with slug already exists."));
        Organization organization = new Organization();

        organization.setName(name);
        organization.setSlug(slug);
        organization.setDescription(description);
        organization.setContactEmail(contactEmail);
        organization.setContactPhone(contactPhone);
        organization.setWebsiteUrl(websiteUrl);
        organization.setAddress(address);
        organization.setLogoUrl(logoUrl);
        organization.setCreatedBy(creator);
        organization.setAdmin(creator);
       // create Organisation membership for the creator user

        return EntityDTOMapper.toOrganizationResponseDTO(organization);
    }









}
