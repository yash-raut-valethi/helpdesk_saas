package com.example.helpdesk_saas.controller;


import com.example.helpdesk_saas.dto.OrganizationRequestDTO;
import com.example.helpdesk_saas.dto.OrganizationResponseDTO;
import com.example.helpdesk_saas.service.OrganizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/organization")
public class OrganizationController {
    private final OrganizationService organizationService;


    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @PostMapping("/create")
    public ResponseEntity<OrganizationResponseDTO> createOrganization(@RequestBody OrganizationRequestDTO requestDTO){
        OrganizationResponseDTO responseDTO = organizationService.createOrganization(requestDTO.getName(), requestDTO.getDescription(), requestDTO.getSlug(), requestDTO.getContactEmail(), requestDTO.getContactPhone(), requestDTO.getWebsiteUrl(), requestDTO.getLogoUrl(), requestDTO.getAddress(), requestDTO.getCreatorEmail());
        return ResponseEntity.ok(responseDTO);
    }

//    @PatchMapping("/update")
//    public ResponseEntity<OrganizationResponseDTO> updateOrganization(@RequestBody OrganizationRequestDTO requestDTO){
//
//    }

}
