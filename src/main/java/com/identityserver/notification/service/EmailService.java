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

    public void sendPasswordResetEmail(String email, String token) {
        String resetLink = "http://localhost:4200/reset-password?token=" + token;

        log.info("==========================================================================");
        log.info("🔑 [NOTIFICATION - ENVOI D'EMAIL DE RÉINITIALISATION DE MOT DE PASSE]");
        log.info("Pour : {}", email);
        log.info("Lien de réinitialisation : {}", resetLink);
        log.info("Jeton (Token) : {}", token);
        log.info("⏰ Ce lien est valide pendant 15 minutes uniquement.");
        log.info("==========================================================================");
    }
}
