package com.identityserver.auth.controller;

import com.identityserver.auth.dto.*;
import com.identityserver.auth.service.AuthService;
import com.identityserver.common.dto.ApiResponse;
import com.identityserver.user.dto.UserResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponseDto>> register(@Valid @RequestBody RegisterRequestDto request) {
        UserResponseDto registeredUser = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Utilisateur enregistré avec succès. Un email de vérification a été envoyé.", registeredUser));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(@Valid @RequestBody LoginRequestDto request) {
        LoginResponseDto loginResponse = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Connexion réussie", loginResponse));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshTokenResponseDto>> refreshToken(@Valid @RequestBody RefreshTokenRequestDto request) {
        RefreshTokenResponseDto refreshedToken = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Jeton d'accès renouvelé avec succès", refreshedToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequestDto request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Déconnexion réussie. Le Refresh Token a été révoqué.", null));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.success("Email vérifié avec succès. Votre compte est maintenant activé.", null));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(@Valid @RequestBody ResendVerificationRequestDto request) {
        authService.resendVerificationEmail(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Un nouvel email de vérification a été envoyé.", null));
    }
}
