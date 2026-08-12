package com.identityserver.auth.controller;

import com.identityserver.auth.dto.RegisterRequestDto;
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
                .body(ApiResponse.success("Utilisateur enregistré avec succès", registeredUser));
    }
}
