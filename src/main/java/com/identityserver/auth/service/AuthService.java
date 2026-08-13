package com.identityserver.auth.service;

import com.identityserver.auth.dto.*;
import com.identityserver.auth.exception.InvalidCredentialsException;
import com.identityserver.role.entity.Role;
import com.identityserver.role.repository.RoleRepository;
import com.identityserver.security.service.CustomUserDetailsService;
import com.identityserver.token.entity.RefreshToken;
import com.identityserver.token.service.JwtService;
import com.identityserver.token.service.RefreshTokenService;
import com.identityserver.user.dto.UserResponseDto;
import com.identityserver.user.entity.User;
import com.identityserver.user.exception.EmailAlreadyExistsException;
import com.identityserver.user.mapper.UserMapper;
import com.identityserver.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final CustomUserDetailsService userDetailsService;

    @Transactional
    public UserResponseDto register(RegisterRequestDto request) {
        // 1. Vérification si l'email existe déjà
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        // 2. Récupération du rôle utilisateur par défaut ROLE_USER
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Erreur d'initialisation : Rôle ROLE_USER introuvable"));

        // 3. Hash du mot de passe avec BCrypt
        String passwordHash = passwordEncoder.encode(request.getPassword());

        // 4. Création de l'entité User avec le rôle ROLE_USER
        User user = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .passwordHash(passwordHash)
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .enabled(true)
                .emailVerified(false)
                .mfaEnabled(false)
                .roles(new HashSet<>(Set.of(userRole)))
                .build();

        // 5. Sauvegarde dans PostgreSQL
        User savedUser = userRepository.save(user);

        // 6. Mapping vers DTO de réponse
        return userMapper.toResponseDto(savedUser);
    }

    @Transactional
    public LoginResponseDto login(LoginRequestDto request) {
        String email = request.getEmail().toLowerCase().trim();

        // 1. Authentification via Spring Security AuthenticationManager
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword())
            );
        } catch (BadCredentialsException ex) {
            throw new InvalidCredentialsException();
        }

        // 2. Récupération de l'utilisateur
        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        // 3. Génération du JWT Access Token et du Refresh Token persistent
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        String jwtToken = jwtService.generateToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        // 4. Construction du DTO de réponse
        return LoginResponseDto.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationTimeSeconds())
                .user(userMapper.toResponseDto(user))
                .build();
    }

    @Transactional
    public RefreshTokenResponseDto refreshToken(RefreshTokenRequestDto request) {
        // 1. Invalidation de l'ancien Refresh Token et création d'un nouveau (Token Rotation)
        RefreshToken newRefreshToken = refreshTokenService.rotateRefreshToken(request.getRefreshToken());

        // 2. Génération d'un nouvel Access Token
        User user = newRefreshToken.getUser();
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String newAccessToken = jwtService.generateToken(userDetails);

        // 3. Retour de la paire de tokens renouvelée
        return RefreshTokenResponseDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationTimeSeconds())
                .build();
    }

    @Transactional
    public void logout(String refreshTokenStr) {
        refreshTokenService.revokeToken(refreshTokenStr);
    }
}
