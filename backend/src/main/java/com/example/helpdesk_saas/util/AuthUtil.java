package com.example.helpdesk_saas.util;


import com.example.helpdesk_saas.exception.UserNotFoundException;
import com.example.helpdesk_saas.models.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuthUtil {


    public static User getCurrentUserFromSession() {
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
