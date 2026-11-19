package com.example.helpdesk_saas.repository;

import com.example.helpdesk_saas.models.Organization;
import com.example.helpdesk_saas.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.JpqlQueryBuilder;

import java.util.List;
import java.util.Optional;

public interface OrganizationRepository  extends JpaRepository<Organization, Long> {

    Optional<Organization> findByNameAndIsDeletedFalse(String name);

    Optional<Organization> findBySlugAndAdmin(String slug, User admin);

    Optional<Organization> findBySlugAndAdminAndIsDeletedFalse(String slug, User admin);

    Optional<Organization> findBySlugAndIsDeletedFalse(String slug);

    List<Organization> findByMemberships_Member_EmailAndIsDeletedFalseAndMemberships_IsDeletedFalse(
            String email
    );

}
