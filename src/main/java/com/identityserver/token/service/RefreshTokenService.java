package com.identityserver.token.service;

import com.identityserver.auth.exception.InvalidRefreshTokenException;
import com.identityserver.token.entity.RefreshToken;
import com.identityserver.token.repository.RefreshTokenRepository;
import com.identityserver.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationMs;

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshExpirationMs))
                .revoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public RefreshToken rotateRefreshToken(String requestRefreshToken) {
        RefreshToken token = refreshTokenRepository.findByToken(requestRefreshToken)
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh Token invalide ou inexistant"));

        User user = token.getUser();

        // 1. Détection de réutilisation suspecte (Token Reuse Detection)
        if (token.isRevoked()) {
            refreshTokenRepository.revokeAllByUser(user);
            throw new InvalidRefreshTokenException("Alerte de sécurité : Tentative de réutilisation d'un Refresh Token révoqué. Toutes vos sessions ont été fermées.");
        }

        // 2. Vérification de la date d'expiration
        if (token.getExpiryDate().isBefore(Instant.now())) {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            throw new InvalidRefreshTokenException("Refresh Token expiré. Veuillez vous re-connecter.");
        }

        // 3. Invalidation du token actuel (Rotation)
        token.setRevoked(true);

        // 4. Génération d'un NOUVEAU Refresh Token
        RefreshToken newRefreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshExpirationMs))
                .revoked(false)
                .build();

        RefreshToken savedNewToken = refreshTokenRepository.save(newRefreshToken);

        token.setReplacedByToken(savedNewToken.getToken());
        refreshTokenRepository.save(token);

        return savedNewToken;
    }

    @Transactional
    public void revokeToken(String tokenStr) {
        refreshTokenRepository.findByToken(tokenStr).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }
}
