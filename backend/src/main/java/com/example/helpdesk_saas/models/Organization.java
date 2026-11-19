package com.example.helpdesk_saas.models;


import com.example.helpdesk_saas.enums.MembershipStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class Organization extends Basemodel{

    @Column(nullable=false, length= 100)
    private String name;
    @Column(nullable = false, length = 50)
    private String slug;

    @Column(length = 1000)
    private String description;

    @Column(length = 255)
    private String contactEmail;

    @Column(length = 30)
    private String contactPhone;

    @Column(length = 255)
    private String websiteUrl;

    @Column(length = 500)
    private String logoUrl;

    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MembershipStatus status = MembershipStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_admin", nullable = false)
    private User admin;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<OrganizationMembership> memberships = new ArrayList<>();


    @Column(name= "is_deleted", nullable=false)
    private boolean isDeleted = false;


    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

}

