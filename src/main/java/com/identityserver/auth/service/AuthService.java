package com.identityserver.auth.service;

import com.identityserver.auth.dto.LoginRequestDto;
import com.identityserver.auth.dto.LoginResponseDto;
import com.identityserver.auth.dto.RegisterRequestDto;
import com.identityserver.auth.exception.InvalidCredentialsException;
import com.identityserver.security.service.CustomUserDetailsService;
import com.identityserver.token.service.JwtService;
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

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Transactional
    public UserResponseDto register(RegisterRequestDto request) {
        // 1. Vérification si l'email existe déjà
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        // 2. Hash du mot de passe avec BCrypt
        String passwordHash = passwordEncoder.encode(request.getPassword());

        // 3. Création de l'entité User
        User user = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .passwordHash(passwordHash)
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .enabled(true)
                .emailVerified(false)
                .mfaEnabled(false)
                .build();

        // 4. Sauvegarde dans PostgreSQL
        User savedUser = userRepository.save(user);

        // 5. Mapping vers DTO de réponse
        return userMapper.toResponseDto(savedUser);
    }

    @Transactional(readOnly = true)
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

        // 3. Génération du JWT Access Token
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        String jwtToken = jwtService.generateToken(userDetails);

        // 4. Construction du DTO de réponse
        return LoginResponseDto.builder()
                .accessToken(jwtToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationTimeSeconds())
                .user(userMapper.toResponseDto(user))
                .build();
    }
}
