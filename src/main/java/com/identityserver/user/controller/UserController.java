package com.identityserver.user.controller;

import com.identityserver.common.dto.ApiResponse;
import com.identityserver.user.dto.UserResponseDto;
import com.identityserver.user.entity.User;
import com.identityserver.user.mapper.UserMapper;
import com.identityserver.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('USER_READ')")
    public ResponseEntity<ApiResponse<UserResponseDto>> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        return ResponseEntity.ok(ApiResponse.success("Profil utilisateur récupéré", userMapper.toResponseDto(user)));
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponseDto>>> getAllUsers() {
        List<UserResponseDto> users = userRepository.findAll().stream()
                .map(userMapper::toResponseDto)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Liste des utilisateurs (Admin)", users));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_DELETE')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable UUID id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        userRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Utilisateur supprimé avec succès", null));
    }
}
