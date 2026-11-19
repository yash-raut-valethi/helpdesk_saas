package com.example.helpdesk_saas.util;

import com.example.helpdesk_saas.dto.LoginResponse;
import com.example.helpdesk_saas.exception.InvalidCredentialsException;
import com.example.helpdesk_saas.service.MailService;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.security.SecureRandom;

public class EmailUtil {
    private static final SecureRandom secureRandom = new SecureRandom();


    public static String generateOTP(){
        StringBuilder otp = new StringBuilder(6);
        for(int i=1;i<=6;i++){
            otp.append(secureRandom.nextInt(10));
        }
        return otp.toString();
    }
}
