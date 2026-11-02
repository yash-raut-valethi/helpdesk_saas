package com.example.helpdesk_saas.models;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Data;

@Entity
@Data
public class Department extends Basemodel {

    @ManyToOne
    private Organization organization;

    private String departmentName;

    private String departmentDescription;

}
