# 🔐 Identity & Authentication Server

Serveur centralisé d'identité et d'authentification construit avec **Spring Boot**, inspiré de solutions professionnelles comme **Auth0**, **Keycloak** ou **Firebase Auth**.

Ce projet a pour objectif de démontrer une compréhension approfondie des mécanismes de sécurité backend : hachage **BCrypt**, signature **JWT (HMAC-SHA256)**, rotation des tokens, **RBAC** (Role-Based Access Control), vérification d'email, et bien plus — sans dépendre d'une solution "boîte noire" tierce.

![Java](https://img.shields.io/badge/Java-17+-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Database-blue)
![JWT](https://img.shields.io/badge/Auth-JWT-black)
![Status](https://img.shields.io/badge/Status-In%20Progress-yellow)

---

## 🎯 Pourquoi ce projet ?

| Raison | Détails |
|---|---|
| **Compétence recherchée** | L'authentification et la sécurité comptent parmi les compétences backend les plus demandées (Spring Security, JWT, OAuth2). |
| **Architecture réelle** | Séparation claire entre l'authentification et le code métier : une application e-commerce ne gère jamais les mots de passe elle-même, elle délègue tout à l'Identity Server. |
| **Compréhension profonde** | Plutôt que d'utiliser Auth0 comme boîte noire, ce projet implémente lui-même le hachage BCrypt, la signature JWT HMAC-SHA256, la rotation des tokens et le RBAC. |
| **Portfolio avancé** | Démontre un niveau ingénieur : monolithe modulaire, sécurité multicouche, gestion de sessions, audit, MFA, OAuth2. |
| **Réutilisabilité** | L'Identity Server peut servir plusieurs applications clientes (E-commerce, Admin Panel, Mobile App...) via JWT et API Keys. |

---

## 🏗️ Architecture du projet

```
identity-server/
├── pom.xml, mvnw.cmd, .env, .env.example, .gitignore
└── src/main/java/com/identityserver/
    ├── IdentityServerApplication.java
    │
    ├── auth/                          # Authentification
    │   ├── controller/AuthController
    │   ├── service/AuthService
    │   ├── dto/ (Register, Login, Refresh, Resend...)
    │   └── exception/ (InvalidCredentials, InvalidRefreshToken, InvalidVerificationToken)
    │
    ├── user/                          # Utilisateurs
    │   ├── entity/User
    │   ├── repository/UserRepository
    │   ├── dto/UserResponseDto
    │   ├── mapper/UserMapper
    │   ├── controller/UserController
    │   └── exception/EmailAlreadyExistsException
    │
    ├── role/                          # Rôles (RBAC)
    │   ├── entity/Role
    │   └── repository/RoleRepository
    │
    ├── permission/                    # Permissions (RBAC)
    │   ├── entity/Permission
    │   └── repository/PermissionRepository
    │
    ├── token/                         # JWT & Refresh Tokens
    │   ├── entity/RefreshToken
    │   ├── repository/RefreshTokenRepository
    │   └── service/ (JwtService, RefreshTokenService)
    │
    ├── notification/                  # Vérification d'email
    │   ├── entity/VerificationToken
    │   ├── repository/VerificationTokenRepository
    │   └── service/ (EmailService, VerificationTokenService)
    │
    ├── security/                      # Spring Security
    │   ├── config/ (SecurityConfig, PasswordEncoderConfig)
    │   ├── jwt/ (JwtAuthenticationFilter, JwtAuthenticationEntryPoint)
    │   └── service/CustomUserDetailsService
    │
    └── common/                        # Utilitaires
        ├── config/DataInitializer
        ├── dto/ (ApiResponse, ErrorResponse)
        └── exception/GlobalExceptionHandler
```

---

## 📊 Modèle de données (PostgreSQL)

| Table | Description |
|---|---|
| `users` | Utilisateurs (email, passwordHash, firstName, lastName, emailVerified...) |
| `roles` | Rôles (`ROLE_USER`, `ROLE_MODERATOR`, `ROLE_ADMIN`) |
| `permissions` | Permissions (`USER_READ`, `USER_CREATE`, `USER_UPDATE`, `USER_DELETE`) |
| `role_permissions` | Table de jointure Rôle ↔ Permission |
| `user_roles` | Table de jointure User ↔ Rôle |
| `refresh_tokens` | Refresh tokens persistants (token, revoked, expiryDate, replacedByToken) |
| `verification_tokens` | Tokens de vérification email (token, used, expiryDate) |
| `password_reset_tokens` | Tokens de réinitialisation de mot de passe (token, used, expiryDate) |

---

## 📋 Endpoints disponibles

### 🟢 Endpoints publics — `/api/auth/**` (aucun JWT requis)

| Méthode | Endpoint | Phase | Description | Body |
|---|---|---|---|---|
| `POST` | `/api/auth/register` | 1 | Créer un nouveau compte utilisateur | `{ email, password, firstName, lastName }` |
| `POST` | `/api/auth/login` | 2 | Se connecter et recevoir un Access Token + Refresh Token | `{ email, password }` |
| `POST` | `/api/auth/refresh` | 3 | Renouveler l'Access Token via rotation du Refresh Token | `{ refreshToken }` |
| `POST` | `/api/auth/logout` | 3 | Révoquer le Refresh Token (déconnexion) | `{ refreshToken }` |
| `GET` | `/api/auth/verify-email?token=xxx` | 5 | Confirmer l'email via le lien de vérification | Query param `token` |
| `POST` | `/api/auth/resend-verification` | 5 | Renvoyer un email de vérification | `{ email }` |
| `POST` | `/api/auth/forgot-password` | 6 | Demander un lien de réinitialisation de mot de passe (valide 15 min) | `{ email }` |
| `POST` | `/api/auth/reset-password` | 6 | Réinitialiser le mot de passe via le token reçu par email | `{ token, newPassword }` |

### 🔒 Endpoints protégés — JWT requis (`Authorization: Bearer <token>`)

| Méthode | Endpoint | Phase | Permission requise | Description |
|---|---|---|---|---|
| `GET` | `/api/users/me` | 4 | `USER_READ` | Récupérer son propre profil |
| `GET` | `/api/users/all` | 4 | `ROLE_ADMIN` | Lister tous les utilisateurs (admin uniquement) |
| `DELETE` | `/api/users/{id}` | 4 | `USER_DELETE` | Supprimer un utilisateur (admin uniquement) |

---

## 🚀 Démarrage rapide

### Prérequis

- Java 17+
- Maven (ou le wrapper `mvnw` fourni)
- PostgreSQL

### Installation

```bash
# Cloner le repo
git clone https://github.com/AlaAydi/Identity_server.git
cd Identity_server

# Copier le fichier d'environnement et renseigner vos variables
cp .env.example .env

# Lancer l'application
./mvnw spring-boot:run
```

Le serveur démarre par défaut sur `http://localhost:8081`.

> ⚠️ Pensez à configurer votre base PostgreSQL et vos variables d'environnement (`.env`) : URL de connexion, secret JWT, identifiants du compte admin, etc.

---

## 🐳 Lancer avec Docker

Le serveur est également disponible en image Docker prête à l'emploi sur Docker Hub, ce qui évite d'installer Java/Maven localement.

### Option A — Utiliser l'image publiée (le plus simple)

```bash
docker pull aydiala/identity-server
```

```bash
docker run -p 8081:8081 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/identity_server \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=votre_mot_de_passe \
  -e JWT_SECRET=votre_cle_secrete_256_bits \
  -e JWT_EXPIRATION=900000 \
  -e JWT_REFRESH_EXPIRATION=604800000 \
  aydiala/identity-server
```

> ℹ️ `host.docker.internal` permet au conteneur de joindre une base PostgreSQL qui tourne sur votre machine hôte (hors conteneur). Si votre base est ailleurs (conteneur, serveur distant), remplacez cette valeur par la bonne adresse.

Vous pouvez aussi réutiliser directement votre `.env` existant :
```bash
docker run -p 8081:8081 --env-file .env \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/identity_server \
  aydiala/identity-server
```

### Option B — Construire l'image vous-même depuis le code source

```bash
git clone https://github.com/AlaAydi/Identity_server.git
cd Identity_server

# Construire l'image (build multi-stage : compilation Maven puis image finale légère)
docker build -t identity-server .

# Lancer le conteneur
docker run -p 8081:8081 --env-file .env \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/identity_server \
  identity-server
```

Le `Dockerfile` utilise un build en 2 étapes : la première compile le `.jar` avec Maven (`maven:3.9-eclipse-temurin-21`), la seconde ne conserve que le `.jar` dans une image `eclipse-temurin:21-jre-alpine` légère, exécutée avec un utilisateur non-root (`spring`) pour plus de sécurité.

### Vérifier que ça fonctionne

```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'
```
Une réponse `401 Unauthorized` (identifiants incorrects) confirme que le serveur, Spring Security et la connexion PostgreSQL fonctionnent correctement.

---

## 🧪 Tester l'API avec Postman

### 1. Inscription (Phase 1)

```http
POST http://localhost:8081/api/auth/register
Content-Type: application/json

{
  "email": "test@example.com",
  "password": "Password123!",
  "firstName": "Jean",
  "lastName": "Dupont"
}
```
📧 Le lien de vérification email simulé apparaît dans les logs du serveur.

### 2. Vérification de l'email (Phase 5)

Copiez le token affiché dans les logs et collez-le dans l'URL :

```http
GET http://localhost:8081/api/auth/verify-email?token=LE_TOKEN_DES_LOGS
```

### 3. Connexion (Phase 2)

```http
POST http://localhost:8081/api/auth/login
Content-Type: application/json

{
  "email": "test@example.com",
  "password": "Password123!"
}
```
Récupérez `accessToken` et `refreshToken` dans la réponse.

### 4. Accéder à une route protégée (Phase 4)

```http
GET http://localhost:8081/api/users/me
Authorization: Bearer <votre_accessToken>
```

### 5. Rafraîchir le token (Phase 3)

```http
POST http://localhost:8081/api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "<votre_refreshToken>"
}
```
⚡ Vous recevez un **nouvel** accessToken et un **nouveau** refreshToken ; l'ancien est automatiquement révoqué.

### 6. Test RBAC — accès admin (Phase 4)

```http
POST http://localhost:8081/api/auth/login
Content-Type: application/json

{
  "email": "admin@identity.com",
  "password": "Admin123!"
}
```

Puis :

```http
GET http://localhost:8081/api/users/all
Authorization: Bearer <token_admin>
```

### 7. Test RBAC — accès refusé (Phase 4)

Avec un token utilisateur standard :

```http
DELETE http://localhost:8081/api/users/{uuid}
Authorization: Bearer <token_user_normal>
```
❌ Réponse attendue : `403 Forbidden`

### 8. Renvoyer la vérification (Phase 5)

```http
POST http://localhost:8081/api/auth/resend-verification
Content-Type: application/json

{
  "email": "test@example.com"
}
```

### 9. Déconnexion (Phase 3)

```http
POST http://localhost:8081/api/auth/logout
Content-Type: application/json

{
  "refreshToken": "<votre_refreshToken>"
}
```

### 10. Mot de passe oublié (Phase 6)

```http
POST http://localhost:8081/api/auth/forgot-password
Content-Type: application/json

{
  "email": "test@example.com"
}
```
📧 Le lien de réinitialisation simulé (avec le token) apparaît dans les logs du serveur. Il est valide 15 minutes.

### 11. Réinitialisation du mot de passe (Phase 6)

```http
POST http://localhost:8081/api/auth/reset-password
Content-Type: application/json

{
  "token": "LE_TOKEN_DES_LOGS",
  "newPassword": "NouveauPassword123!"
}
```
⚡ Toutes les sessions actives (refresh tokens) de l'utilisateur sont révoquées après la réinitialisation.

---

## ✅ Fonctionnalités implémentées

| Phase | Fonctionnalité | Statut |
|---|---|---|
| 1 | Inscription utilisateur, PostgreSQL, BCrypt, validation, `GlobalExceptionHandler` | ✅ Terminé |
| 2 | Spring Security, JWT (HMAC-SHA256), login, access token, `JwtFilter` | ✅ Terminé |
| 3 | Refresh token, rotation des tokens, détection de réutilisation, logout | ✅ Terminé |
| 4 | Rôles (USER, ADMIN, MODERATOR), permissions, RBAC, `@PreAuthorize`, `DataInitializer` | ✅ Terminé |
| 5 | Vérification d'email, tokens de vérification (24h), renvoi de vérification | ✅ Terminé |
| 6 | Réinitialisation de mot de passe (`forgot-password` / `reset-password`), tokens temporaires (15 min), révocation des sessions actives | ✅ Terminé |

## 🚧 Feuille de route (Phases 7–15)

| Phase | Fonctionnalité | Description |
|---|---|---|
| 7 | Sessions | Voir les sessions actives (device, IP, userAgent), supprimer des sessions |
| 8 | Logs d'audit | Enregistrement de `LOGIN_SUCCESS`, `LOGIN_FAILED`, `PASSWORD_CHANGED`, etc. avec endpoint admin |
| 9 | MFA (TOTP) | Authentification à deux facteurs avec Google Authenticator |
| 10 | Google OAuth2 | Connexion via Google avec Spring Security OAuth2 Client |
| 11 | API Keys | Système de clés API pour les applications externes |
| 12 | Redis | Rate limiting, cache de sessions, gestion des tokens temporaires |
| 13 | Durcissement sécurité | CORS, verrouillage de compte, protection brute-force |
| 14 | Tests | JUnit 5, Mockito, Spring Boot Test |
| 15 | Intégration E-commerce | Connexion d'une application Angular + Spring Boot à l'Identity Server |

---

## 🛠️ Stack technique

- **Langage** : Java 17+
- **Framework** : Spring Boot, Spring Security
- **Base de données** : PostgreSQL
- **Authentification** : JWT (HMAC-SHA256), BCrypt
- **Build** : Maven
- **Conteneurisation** : Docker (build multi-stage, image publiée sur [Docker Hub](https://hub.docker.com/r/aydiala/identity-server))

---

## 📄 Licence

Ce projet est distribué sous licence MIT — voir le fichier [LICENSE](LICENSE) pour plus de détails.

## 👤 Auteur

**Ala Aydi** — [@AlaAydi](https://github.com/AlaAydi)
