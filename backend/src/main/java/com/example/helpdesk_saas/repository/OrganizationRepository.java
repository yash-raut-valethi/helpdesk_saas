package com.example.helpdesk_saas.repository;

import com.example.helpdesk_saas.models.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrganizationRepository  extends JpaRepository<Organization, Long> {

    Optional<Organization> findByName(String name);

    Optional<Organization> findBySlug(String slug);

}
