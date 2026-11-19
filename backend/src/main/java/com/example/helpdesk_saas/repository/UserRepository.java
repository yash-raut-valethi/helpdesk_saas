package com.example.helpdesk_saas.repository;

import com.example.helpdesk_saas.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

Optional<User> findByEmailAndIsDeletedFalse(String email);

}
