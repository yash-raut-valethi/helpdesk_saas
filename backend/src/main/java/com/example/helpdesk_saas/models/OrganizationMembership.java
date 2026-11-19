package com.example.helpdesk_saas.models;

import com.example.helpdesk_saas.enums.OrganizationRole;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "organization_memberships")
@Data
public class OrganizationMembership extends Basemodel {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    private OrganizationRole role;

    private boolean isDeleted = false;

}
