package com.identityserver.common.config;

import com.identityserver.permission.entity.Permission;
import com.identityserver.permission.repository.PermissionRepository;
import com.identityserver.role.entity.Role;
import com.identityserver.role.repository.RoleRepository;
import com.identityserver.user.entity.User;
import com.identityserver.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Initialisation des Rôles, Permissions et Admin par défaut...");

        // 1. Initialisation des Permissions
        Permission userRead = createPermissionIfNotFound("USER_READ", "Autorisation de lecture des données utilisateur");
        Permission userCreate = createPermissionIfNotFound("USER_CREATE", "Autorisation de création d'utilisateurs");
        Permission userUpdate = createPermissionIfNotFound("USER_UPDATE", "Autorisation de mise à jour des utilisateurs");
        Permission userDelete = createPermissionIfNotFound("USER_DELETE", "Autorisation de suppression d'utilisateurs");

        // 2. Initialisation des Rôles
        Role roleUser = createRoleIfNotFound("ROLE_USER", "Rôle utilisateur standard", Set.of(userRead));
        Role roleModerator = createRoleIfNotFound("ROLE_MODERATOR", "Rôle modérateur", Set.of(userRead, userUpdate));
        Role roleAdmin = createRoleIfNotFound("ROLE_ADMIN", "Rôle administrateur système", Set.of(userRead, userCreate, userUpdate, userDelete));

        // 3. Compte Admin par défaut
        if (!userRepository.existsByEmail("admin@identity.com")) {
            User admin = User.builder()
                    .email("admin@identity.com")
                    .passwordHash(passwordEncoder.encode("Admin123!"))
                    .firstName("Admin")
                    .lastName("System")
                    .enabled(true)
                    .emailVerified(true)
                    .mfaEnabled(false)
                    .roles(new HashSet<>(Set.of(roleAdmin)))
                    .build();
            userRepository.save(admin);
            log.info("Compte Admin créé avec succès : admin@identity.com / Admin123!");
        }

        log.info("Initialisation terminée avec succès.");
    }

    private Permission createPermissionIfNotFound(String name, String description) {
        return permissionRepository.findByName(name)
                .orElseGet(() -> permissionRepository.save(
                        Permission.builder().name(name).description(description).build()
                ));
    }

    private Role createRoleIfNotFound(String name, String description, Set<Permission> permissions) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(
                        Role.builder()
                                .name(name)
                                .description(description)
                                .permissions(new HashSet<>(permissions))
                                .build()
                ));
    }
}
