package com.example.helpdesk_saas.repository;

import com.example.helpdesk_saas.models.OrganizationMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrganizationMembershipRepository extends JpaRepository<OrganizationMembership,Long>
{

}
