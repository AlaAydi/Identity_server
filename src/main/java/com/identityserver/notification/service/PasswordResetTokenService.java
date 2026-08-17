package com.identityserver.notification.service;

import com.identityserver.auth.exception.InvalidPasswordResetTokenException;
import com.identityserver.notification.entity.PasswordResetToken;
import com.identityserver.notification.repository.PasswordResetTokenRepository;
import com.identityserver.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetTokenService {

    private static final long EXPIRATION_MINUTES = 15;

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Transactional
    public PasswordResetToken createPasswordResetToken(User user) {
        // Supprimer l'ancien token de réinitialisation s'il existe
        passwordResetTokenRepository.findByUser(user).ifPresent(passwordResetTokenRepository::delete);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plus(EXPIRATION_MINUTES, ChronoUnit.MINUTES))
                .used(false)
                .build();

        return passwordResetTokenRepository.save(resetToken);
    }

    @Transactional
    public User validateAndGetUser(String tokenStr) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(tokenStr)
                .orElseThrow(() -> new InvalidPasswordResetTokenException(
                        "Jeton de réinitialisation invalide ou inexistant"));

        if (resetToken.isUsed()) {
            throw new InvalidPasswordResetTokenException(
                    "Ce jeton de réinitialisation a déjà été utilisé");
        }

        if (resetToken.getExpiryDate().isBefore(Instant.now())) {
            throw new InvalidPasswordResetTokenException(
                    "Le jeton de réinitialisation a expiré (valide 15 minutes). Veuillez refaire une demande.");
        }

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        return resetToken.getUser();
    }
}
