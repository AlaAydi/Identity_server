package com.identityserver.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    public void sendVerificationEmail(String email, String token) {
        String verificationLink = "http://localhost:8081/api/auth/verify-email?token=" + token;

        log.info("==========================================================================");
        log.info("📧 [NOTIFICATION - ENVOI D'EMAIL DE VÉRIFICATION]");
        log.info("Pour : {}", email);
        log.info("Lien de confirmation : {}", verificationLink);
        log.info("Jeton (Token) : {}", token);
        log.info("==========================================================================");
    }
}
