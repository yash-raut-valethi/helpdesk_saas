package com.example.helpdesk_saas.controller;


import com.example.helpdesk_saas.dto.*;
import com.example.helpdesk_saas.models.OrganizationMembership;
import com.example.helpdesk_saas.service.OrganizationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/organization")
public class OrganizationController {
    private final OrganizationService organizationService;


    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @PostMapping("/create")
    public ResponseEntity<OrganizationResponseDTO> createOrganization(@Valid @RequestBody OrganizationRequestDTO requestDTO){
        OrganizationResponseDTO responseDTO = organizationService.createOrganization(requestDTO.getName(), requestDTO.getSlug(), requestDTO.getDescription(), requestDTO.getContactEmail(), requestDTO.getContactPhone(), requestDTO.getWebsiteUrl(), requestDTO.getLogoUrl(), requestDTO.getAddress());
        return ResponseEntity.ok(responseDTO);
    }

    @PatchMapping("/update")
    public ResponseEntity<OrganizationResponseDTO> editOrganizationDetails(@Valid @RequestBody OrganizationUpdateRequestDTO requestDTO){
        OrganizationResponseDTO responseDTO = organizationService.editOrganizationDetails(requestDTO.getName(), requestDTO.getSlug(),  requestDTO.getDescription(), requestDTO.getContactEmail(), requestDTO.getContactPhone(), requestDTO.getWebsiteUrl(), requestDTO.getLogoUrl(), requestDTO.getAddress());
        return ResponseEntity.ok(responseDTO);
    }

    @GetMapping("/fetch/all")
    public ResponseEntity<List<OrganizationResponseDTO>> getAllOrganizations(){
        List<OrganizationResponseDTO> responseDTOs = organizationService.getOrganizations();
        return ResponseEntity.ok(responseDTOs);
    }

    @DeleteMapping("/deActivate-organization")
    public ResponseEntity<DeletedOrganizationResponseDTO> deActivateOrganization(@Valid @RequestBody OrganizationRequestDTO requestDTO){

       DeletedOrganizationResponseDTO responseDTO = organizationService.deActivateOrganization(requestDTO.getSlug());
        return ResponseEntity.ok(responseDTO);
    }

    @PostMapping("/transfer/admin/request")
    public ResponseEntity<String> transferAdminRequest( @Valid @RequestBody AdminTransferRequestDTO requestDTO){
        String response = organizationService.verifyAdminTransferRequest(requestDTO.getCurrentUserEmail(), requestDTO.getSlug());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/transfer/admin/request/validate")
    public ResponseEntity<Void> transferAdminRequestValidate(@Valid @RequestBody AdminTransferRequestValidateDTO requestDTO){
        String response = organizationService.verifyAdminTransferRequestValidate(requestDTO.getCurrentUserEmail(), requestDTO.getOtp());
        return ResponseEntity.ok(response);
    }



}
