package com.example.helpdesk_saas.service;


import com.example.helpdesk_saas.dto.ProfileResponseDTO;
import com.example.helpdesk_saas.exception.InvalidCredentialsException;
import com.example.helpdesk_saas.exception.UserNotFoundException;
import com.example.helpdesk_saas.models.User;
import com.example.helpdesk_saas.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;



    public ProfileService(PasswordEncoder passwordEncoder, UserRepository userRepository) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    public ProfileResponseDTO getUserDetails() {
        User user = getCurrentUserFromSession();
        ProfileResponseDTO profileDTO = new ProfileResponseDTO();
        profileDTO.setEmail(user.getEmail());
        profileDTO.setName(user.getName());
        return profileDTO;
    }


    public void resetPassword(String password){
        User user = getCurrentUserFromSession();
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
    }


    public void verifyCurrentPassword(String password){
        User user =   getCurrentUserFromSession();
        if(!passwordEncoder.matches(password,user.getPassword())){
            throw new InvalidCredentialsException("Invalid Credentials");
        }
    }



    private User getCurrentUserFromSession() {
        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        if (auth == null
                || !auth.isAuthenticated()
                || auth instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Not authenticated");
        }
        User user = (User) auth.getPrincipal();
        if(user==null){
            throw new UserNotFoundException("No user found in current session.");
        }
        return user;
    }

}
