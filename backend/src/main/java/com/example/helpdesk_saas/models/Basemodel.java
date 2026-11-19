package com.example.helpdesk_saas.models;


import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;

@MappedSuperclass
@Data
public class Basemodel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


}
