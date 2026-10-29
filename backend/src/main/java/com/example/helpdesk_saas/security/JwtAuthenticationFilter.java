package com.example.helpdesk_saas.security;

import com.example.helpdesk_saas.models.User;
import com.example.helpdesk_saas.repository.BlackListedAccessTokenRepository;
import com.example.helpdesk_saas.repository.UserRepository;

import io.jsonwebtoken.JwtException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.servlet.HandlerExceptionResolver;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final BlackListedAccessTokenRepository blacklistRepository;
    private final HandlerExceptionResolver exceptionResolver;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository,
            BlackListedAccessTokenRepository blacklistRepository,
            @Qualifier("handlerExceptionResolver")
            HandlerExceptionResolver exceptionResolver) {

        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.blacklistRepository = blacklistRepository;
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String token = null;

        // 1. Check Authorization header first.
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }

        // 2. If absent, check the cookie.
        if (token == null || token.isBlank()) {

            Cookie[] cookies = request.getCookies();

            if (cookies != null) {
                for (Cookie cookie : cookies) {

                    if ("accessToken".equals(cookie.getName())) {
                        token = cookie.getValue();
                        break;
                    }
                }
            }
        }

        // 3. No token: let Spring Security handle authorization.
        if (token == null || token.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {

            // 4. Check revocation regardless of token source.
            boolean blacklisted =
                    blacklistRepository.findByToken(token).isPresent();

            if (blacklisted) {
                rejectToken(response);
                return;
            }

            // 5. Validate JWT and identify user.
            String email = jwtService.extractEmail(token);

            if (email == null || email.isBlank()) {
                rejectToken(response);
                return;
            }

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() ->
                            new UsernameNotFoundException("User not found"));

            if (!jwtService.isTokenValid(token, user.getEmail())) {
                rejectToken(response);
                return;
            }

            // 6. Establish authentication.
            Authentication existingAuth =
                    SecurityContextHolder.getContext().getAuthentication();

            if (existingAuth == null) {

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                user,
                                null,
                                List.of(
                                        new SimpleGrantedAuthority("ROLE_USER")
                                )
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);
            }

        }  catch (JwtException
         | IllegalArgumentException
         | UsernameNotFoundException e) {

        SecurityContextHolder.clearContext();
        rejectToken(response);
        return;

    } catch (Exception e) {

        SecurityContextHolder.clearContext();

        exceptionResolver.resolveException(
                request,
                response,
                null,
                e
        );

        return;
    }

        // 7. Token valid and not revoked.
        filterChain.doFilter(request, response);
    }

    private void rejectToken(HttpServletResponse response)
            throws IOException {

        SecurityContextHolder.clearContext();

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        response.getWriter().write(
                "{\"error\":\"Invalid, expired, or revoked token\"}"
        );

        response.getWriter().flush();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {

        String path = request.getServletPath();

        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || "/auth/login".equals(path)
                || "/auth/register".equals(path)
                || "/auth/forgot-password".equals(path)
                || "/auth/forgot-password/otp/verify".equals(path)
                || "/auth/refresh".equals(path)
                || "/auth/logout".equals(path);
    }
}