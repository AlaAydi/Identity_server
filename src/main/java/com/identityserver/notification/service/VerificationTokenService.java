package com.identityserver.notification.service;

import com.identityserver.auth.exception.InvalidVerificationTokenException;
import com.identityserver.notification.entity.VerificationToken;
import com.identityserver.notification.repository.VerificationTokenRepository;
import com.identityserver.user.entity.User;
import com.identityserver.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VerificationTokenService {

    private static final long EXPIRATION_HOURS = 24;

    private final VerificationTokenRepository verificationTokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public VerificationToken createVerificationToken(User user) {
        // Supprimer l'ancien jeton de vérification s'il existe
        verificationTokenRepository.findByUser(user).ifPresent(verificationTokenRepository::delete);

        VerificationToken verificationToken = VerificationToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plus(EXPIRATION_HOURS, ChronoUnit.HOURS))
                .used(false)
                .build();

        return verificationTokenRepository.save(verificationToken);
    }

    @Transactional
    public User verifyEmailToken(String tokenStr) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(tokenStr)
                .orElseThrow(() -> new InvalidVerificationTokenException("Jeton de vérification invalide ou inexistant"));

        if (verificationToken.isUsed()) {
            throw new InvalidVerificationTokenException("Ce jeton de vérification a déjà été utilisé");
        }

        if (verificationToken.getExpiryDate().isBefore(Instant.now())) {
            throw new InvalidVerificationTokenException("Le jeton de vérification a expiré. Veuillez demander un nouvel email de vérification.");
        }

        verificationToken.setUsed(true);
        verificationTokenRepository.save(verificationToken);

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        return userRepository.save(user);
    }
}
