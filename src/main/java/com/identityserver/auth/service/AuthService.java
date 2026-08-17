package com.identityserver.auth.service;

import com.identityserver.auth.dto.*;
import com.identityserver.auth.exception.InvalidCredentialsException;
import com.identityserver.auth.exception.InvalidVerificationTokenException;
import com.identityserver.notification.entity.PasswordResetToken;
import com.identityserver.notification.entity.VerificationToken;
import com.identityserver.notification.service.EmailService;
import com.identityserver.notification.service.PasswordResetTokenService;
import com.identityserver.notification.service.VerificationTokenService;
import com.identityserver.role.entity.Role;
import com.identityserver.role.repository.RoleRepository;
import com.identityserver.security.service.CustomUserDetailsService;
import com.identityserver.token.entity.RefreshToken;
import com.identityserver.token.repository.RefreshTokenRepository;
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
    private final RefreshTokenRepository refreshTokenRepository;
    private final CustomUserDetailsService userDetailsService;
    private final VerificationTokenService verificationTokenService;
    private final PasswordResetTokenService passwordResetTokenService;
    private final EmailService emailService;

    @Transactional
    public UserResponseDto register(RegisterRequestDto request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Erreur d'initialisation : Rôle ROLE_USER introuvable"));

        String passwordHash = passwordEncoder.encode(request.getPassword());

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

        User savedUser = userRepository.save(user);

        VerificationToken verificationToken = verificationTokenService.createVerificationToken(savedUser);
        emailService.sendVerificationEmail(savedUser.getEmail(), verificationToken.getToken());

        return userMapper.toResponseDto(savedUser);
    }

    @Transactional
    public LoginResponseDto login(LoginRequestDto request) {
        String email = request.getEmail().toLowerCase().trim();

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword())
            );
        } catch (BadCredentialsException ex) {
            throw new InvalidCredentialsException();
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        String jwtToken = jwtService.generateToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

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
        RefreshToken newRefreshToken = refreshTokenService.rotateRefreshToken(request.getRefreshToken());

        User user = newRefreshToken.getUser();
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String newAccessToken = jwtService.generateToken(userDetails);

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

    @Transactional
    public void verifyEmail(String token) {
        verificationTokenService.verifyEmailToken(token);
    }

    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new InvalidVerificationTokenException("Aucun utilisateur trouvé avec cet email"));

        if (user.isEmailVerified()) {
            throw new InvalidVerificationTokenException("Cet email est déjà vérifié");
        }

        VerificationToken verificationToken = verificationTokenService.createVerificationToken(user);
        emailService.sendVerificationEmail(user.getEmail(), verificationToken.getToken());
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequestDto request) {
        String email = request.getEmail().toLowerCase().trim();

        userRepository.findByEmail(email).ifPresent(user -> {
            PasswordResetToken resetToken = passwordResetTokenService.createPasswordResetToken(user);
            emailService.sendPasswordResetEmail(user.getEmail(), resetToken.getToken());
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequestDto request) {
        User user = passwordResetTokenService.validateAndGetUser(request.getToken());

        String newPasswordHash = passwordEncoder.encode(request.getNewPassword());
        user.setPasswordHash(newPasswordHash);
        userRepository.save(user);

        refreshTokenRepository.revokeAllByUser(user);
    }
}
