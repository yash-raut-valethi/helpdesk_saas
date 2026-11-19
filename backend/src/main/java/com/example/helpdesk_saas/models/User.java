package com.example.helpdesk_saas.models;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@Table(name= "users")
@Entity
public class User extends Basemodel{

    private String name;
    private String email;
    private String password;

    @OneToMany(mappedBy = "member")
    private List<OrganizationMembership> memberships = new ArrayList<>();

}
