package com.identityserver.user.mapper;

import com.identityserver.permission.entity.Permission;
import com.identityserver.role.entity.Role;
import com.identityserver.user.dto.UserResponseDto;
import com.identityserver.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserResponseDto toResponseDto(User user) {
        if (user == null) {
            return null;
        }

        Set<String> roles = user.getRoles() != null ?
                user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()) :
                Collections.emptySet();

        Set<String> permissions = user.getRoles() != null ?
                user.getRoles().stream()
                        .flatMap(role -> role.getPermissions().stream())
                        .map(Permission::getName)
                        .collect(Collectors.toSet()) :
                Collections.emptySet();

        return UserResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .enabled(user.isEnabled())
                .emailVerified(user.isEmailVerified())
                .mfaEnabled(user.isMfaEnabled())
                .roles(roles)
                .permissions(permissions)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
