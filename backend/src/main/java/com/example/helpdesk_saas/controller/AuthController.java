package com.example.helpdesk_saas.controller;


import com.example.helpdesk_saas.dto.*;
import com.example.helpdesk_saas.service.AuthService;
import com.example.helpdesk_saas.service.RefreshTokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    @Value("${jwt.expiry-ms}")
    private long accessTokenExpiration;
    @Value("${jwt.refresh-expiration-ms}")
    private long refreshTokenExpiration;

    public AuthController(AuthService authService, RefreshTokenService refreshTokenService) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@Valid @RequestBody RegisterDTO registerDTO){
        String response =
      authService.registerUser(registerDTO.getName(),registerDTO.getEmail(), registerDTO.getPassword());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
   public ResponseEntity<String> loginUser(@Valid @RequestBody LoginDTO loginDTO, HttpServletResponse response){
        LoginResponse loginResponse =
                authService.loginUser(loginDTO.getEmail(),loginDTO.getPassword());

        ResponseCookie jwtCookie = ResponseCookie.from("accessToken", loginResponse.getJwtToken())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(Duration.ofMillis(accessTokenExpiration))
                .sameSite("Lax")
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", loginResponse.getRefreshToken())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(Duration.ofMillis(refreshTokenExpiration))
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        return ResponseEntity.ok("Login Successful");
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logoutUser(HttpServletRequest request, HttpServletResponse response){
        String token = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }
        // Revoke the token if present.
        if (token != null && !token.isBlank()) {
            authService.logout(token);
        }

        // Clear the browser cookie.
        ResponseCookie clearAccessTokenCookie = ResponseCookie
                .from("accessToken", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ZERO)
                .build();


        ResponseCookie clearRefreshCookie = ResponseCookie
                .from("refreshToken", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ZERO)
                .build();

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                clearAccessTokenCookie.toString()
        );

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                clearRefreshCookie.toString()
        );
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO requestDTO){
        String response = authService.forgotPassword(requestDTO.getEmail());
        return ResponseEntity.ok(response);
    }


    @PostMapping("/forgot-password/otp/verify")
    public ResponseEntity<String> verifyLoginOTP(@RequestBody ForgotPasswordOTPRequestDTO requestDTO, HttpServletResponse response){
        LoginResponse token = authService.verifyLoginOTP(requestDTO.getEmail(), requestDTO.getOtp());

        ResponseCookie accessCookie = ResponseCookie.from("accessToken", token.getJwtToken())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(Duration.ofMillis(accessTokenExpiration))
                .sameSite("Lax")
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", token.getRefreshToken())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(Duration.ofMillis(refreshTokenExpiration))
                .sameSite("Lax")
                .build();


        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        return ResponseEntity.ok("Login Successful");
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshAccessToken(@CookieValue(name = "refreshToken", required = false) String refreshToken, HttpServletResponse response) {
        TokenRefreshResponseDTO refreshTokenResponse = refreshTokenService
                .refreshAccessToken(refreshToken);

        ResponseCookie jwtCookie = ResponseCookie.from("accessToken", refreshTokenResponse.getAccessToken())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(Duration.ofMillis(accessTokenExpiration))
                .sameSite("Lax")
                .build();
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshTokenResponse.getRefreshToken())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(Duration.ofMillis(refreshTokenExpiration))
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        return ResponseEntity.ok(HttpStatus.CREATED);
    }

}





