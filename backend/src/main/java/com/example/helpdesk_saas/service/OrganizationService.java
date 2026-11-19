package com.example.helpdesk_saas.service;

import com.example.helpdesk_saas.dto.DeletedOrganizationResponseDTO;
import com.example.helpdesk_saas.dto.OrganizationResponseDTO;
import com.example.helpdesk_saas.enums.OrganizationRole;
import com.example.helpdesk_saas.exception.*;
import com.example.helpdesk_saas.models.Organization;
import com.example.helpdesk_saas.models.OrganizationMembership;
import com.example.helpdesk_saas.models.User;
import com.example.helpdesk_saas.repository.OrganizationMembershipRepository;
import com.example.helpdesk_saas.repository.OrganizationRepository;
import com.example.helpdesk_saas.repository.UserRepository;
import com.example.helpdesk_saas.util.EmailUtil;
import com.example.helpdesk_saas.util.EntityDTOMapper;
import jakarta.persistence.*;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.example.helpdesk_saas.util.AuthUtil.getCurrentUserFromSession;



@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final OrganizationMembershipRepository organizationMembershipRepository;
    private final MailService mailService;
    private final RedisTemplate<String,String> redisTemplate;


    public OrganizationService(OrganizationRepository organizationRepository, UserRepository userRepository, OrganizationMembershipRepository organizationMembershipRepository, MailService mailService, RedisTemplate<String,String> redisTemplate) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.organizationMembershipRepository = organizationMembershipRepository;
        this.mailService = mailService;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public OrganizationResponseDTO createOrganization(String name, String slug, String description, String contactEmail, String contactPhone, String websiteUrl, String address, String logoUrl) {
        User currentUser = getCurrentUserFromSession();
        User creator = userRepository.findByEmailAndIsDeletedFalse(currentUser.getEmail()).orElseThrow(() -> new UserNotFoundException("User is not authorized to perform action"));
        if (organizationRepository.findByNameAndIsDeletedFalse(name).isPresent()) {
            throw new OrganizationAlreadyExistException("Organization with this name already exists");
        }
        if (organizationRepository.findBySlugAndIsDeletedFalse(slug).isPresent()) {
            throw new OrganizationAlreadyExistException("Organization with this slug already exists");
        }

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
        organization.setDeleted(false);
        // create Organization membership for the creator user
        OrganizationMembership organizationMembership = new OrganizationMembership();

        organizationMembership.setMember(creator);
        organizationMembership.setOrganization(organization);
        organizationMembership.setRole(OrganizationRole.ADMIN);
        organization.getMemberships().add(organizationMembership);

        organizationRepository.save(organization);
        organizationMembershipRepository.save(organizationMembership);

        return EntityDTOMapper.toOrganizationResponseDTO(organization);
    }

    @Transactional
    public OrganizationResponseDTO editOrganizationDetails(String name, String slug,  String description, String contactEmail, String contactPhone, String websiteUrl, String address, String logoUrl){
        User currentUser = getCurrentUserFromSession();
        Organization organization = organizationRepository.findBySlugAndAdminAndIsDeletedFalse(slug, currentUser).orElseThrow(() -> new OrganizationNotFoundException("Organization not found Or User is not authorized to perform this action"));

        if(name!=null && !name.isBlank() && !name.trim().equals(organization.getName())  && organizationRepository.findByNameAndIsDeletedFalse(name.trim()).isPresent()){
            throw new OrganizationAlreadyExistException("Organization with this name already exists");
        }

        if(name!=null && !name.isBlank())
        {organization.setName(name);}
        if(description!=null && !description.isBlank())
        {organization.setDescription(description);}
        if(contactEmail!=null && !contactEmail.isBlank())
        {organization.setContactEmail(contactEmail);}
        if(contactPhone!=null && !contactPhone.isBlank())
        {organization.setContactPhone(contactPhone);}
        if(websiteUrl!=null && !websiteUrl.isBlank())
        {organization.setWebsiteUrl(websiteUrl);}
        if(address!=null && !address.isBlank())
        {organization.setAddress(address);}
        if(logoUrl!=null && !logoUrl.isBlank())
        {organization.setLogoUrl(logoUrl);}

        organizationRepository.save(organization);
        return EntityDTOMapper.toOrganizationResponseDTO(organization);
    }



@Transactional
public String verifyAdminTransferRequest(String currentUserEmail, String organizationSlug) {
    User currentUser = getCurrentUserFromSession();
    if (!currentUser.getEmail().equals(currentUserEmail)) {
        throw new InvalidCredentialsException("User is not authorized to perform this action");
    }
    Organization organization = organizationRepository.findBySlugAndAdminAndIsDeletedFalse(organizationSlug, currentUser).orElseThrow(() -> new OrganizationNotFoundException("Organization not found Or User is not authorized to perform this action"));

    //send an email for otp verification to the current admin's email id (via dedicated util method)
    String subject = "OTP to verify Admin Action";
    String body = "Hello " + organization.getName()+ " Admin ! ,\n\n";
    sendEmailOtpForVerification(currentUser.getEmail(), subject, body);

    return "Please check the OTP sent to the registered email ID" ;

    }




@Transactional
public List<OrganizationResponseDTO> getOrganizations(){
    User currentUser = getCurrentUserFromSession();
    List<Organization> organizations = organizationRepository.findByMemberships_Member_EmailAndIsDeletedFalseAndMemberships_IsDeletedFalse(currentUser.getEmail());
    List<OrganizationResponseDTO> responseDTOs = new ArrayList<>();
    for(Organization organization: organizations){
        responseDTOs.add(EntityDTOMapper.toOrganizationResponseDTO(organization));
    }
    return responseDTOs;
}


@Transactional
public DeletedOrganizationResponseDTO deActivateOrganization(String slug){
    User currentUser = getCurrentUserFromSession();
    Organization organization = organizationRepository.findBySlugAndAdminAndIsDeletedFalse(slug, currentUser).orElseThrow(()-> new OrganizationNotFoundException("Organization not found Or User is not an Admin"));
    organization.setDeleted(true);
    organizationRepository.save(organization);
    DeletedOrganizationResponseDTO responseDTO =  new DeletedOrganizationResponseDTO();
    responseDTO.setName(organization.getName());
    responseDTO.setSlug(organization.getSlug());
    responseDTO.setContactEmail(organization.getContactEmail());
    responseDTO.setContactPhone(organization.getContactPhone());
    responseDTO.setDescription(organization.getDescription());
    return responseDTO;

}



public void sendEmailOtpForVerification(String to, String subject, String body) {
    try {
        String otp = EmailUtil.generateOTP();
        body += "Below is your OTP for verifying that the action of admin change is intended and authorized by you: \n\n" + otp + "\n\nThis OTP is valid for 5 minutes. Please do not share the otp with anyone.";
        mailService.sendSimpleMail(to,subject,body);
        redisTemplate.opsForValue().set(to, otp,  5,
                TimeUnit.MINUTES);
    }
    catch(Exception e){
        throw new EmailException("Error sending email to the user.");
    }

}

public void verifyAdminTransferRequestValidate(String currentUserEmail, String otp) {

}



}




//
//        // Change Admin update for organization
//        if (newAdminEmail!=null && !newAdminEmail.isBlank()) {
//
//newAdminEmail = newAdminEmail.trim();
//
//User newAdmin = userRepository.findByEmailAndIsDeletedFalse(newAdminEmail)
//        .orElseThrow(() ->
//                new UserNotFoundException("Invalid or unregistered email id entered."));
//
//        if (newAdmin.getId().equals(currentUser.getId())) {
//        throw new IllegalArgumentException("Given account is already an admin");
//        }
//
//List<OrganizationMembership> memberships = organization.getMemberships();
//
//OrganizationMembership previousAdminMembership = memberships.stream()
//        .filter(m -> !m.isDeleted()
//                && m.getMember().getId().equals(currentUser.getId())
//                && m.getRole() == OrganizationRole.ADMIN)
//        .findFirst()
//        .orElseThrow(() ->
//                new OrganizationMemberNotFound("Current admin membership not found"));
//
//OrganizationMembership existingNewAdminMembership = memberships.stream()
//        .filter(m -> !m.isDeleted()
//                && m.getMember().getId().equals(newAdmin.getId()) && m.getRole() == OrganizationRole.AGENT)
//        .findFirst()
//        .orElseThrow(() ->
//                new OrganizationMemberNotFound(
//                        "Email account is not a staff member of the organization."));
//
//
//
//        previousAdminMembership.setDeleted(true);
//        existingNewAdminMembership.setDeleted(true);
//
//        organizationMembershipRepository.save(previousAdminMembership);
//        organizationMembershipRepository.save(existingNewAdminMembership);
//
//// Create AGENT membership for previous admin
//OrganizationMembership previousAdminAgentMembership = new OrganizationMembership();
//        previousAdminAgentMembership.setOrganization(organization);
//        previousAdminAgentMembership.setMember(currentUser);
//        previousAdminAgentMembership.setRole(OrganizationRole.AGENT);
//        previousAdminAgentMembership.setDeleted(false);
//
//
//// Create ADMIN membership for new admin
//OrganizationMembership newAdminMembership = new OrganizationMembership();
//        newAdminMembership.setOrganization(organization);
//        newAdminMembership.setMember(newAdmin);
//        newAdminMembership.setRole(OrganizationRole.ADMIN);
//        newAdminMembership.setDeleted(false);
//
//        organizationMembershipRepository.save(previousAdminAgentMembership);
//        organizationMembershipRepository.save(newAdminMembership);
//
//        organization.getMemberships().add(previousAdminAgentMembership);
//        organization.getMemberships().add(newAdminMembership);
//
//        organization.setAdmin(newAdmin);
//    }
