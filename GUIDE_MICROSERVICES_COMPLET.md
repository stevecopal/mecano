# 🚗 Guide Complet — Plateforme d'Assistance Automobile (Microservices)

> **À qui s'adresse ce guide ?**
> Tu joues le rôle de Tech Lead sur ce projet. Ce document est ta Bible.
> Il explique **pourquoi** chaque décision est prise, **comment** la mettre en place,
> et donne des **exemples concrets** à chaque étape.

---

## Table des matières

1. [Comprendre l'architecture globale](#1-comprendre-larchitecture-globale)
2. [Créer le dépôt GitHub](#2-créer-le-dépôt-github)
3. [Créer la structure du projet](#3-créer-la-structure-du-projet)
4. [Le Discovery Service (Eureka)](#4-le-discovery-service-eureka)
5. [Le Gateway Service](#5-le-gateway-service)
6. [L'Auth Service](#6-lauth-service)
7. [Le User Service](#7-le-user-service)
8. [Dockeriser chaque service](#8-dockeriser-chaque-service)
9. [Le docker-compose.yml global](#9-le-docker-composeyml-global)
10. [Le fichier .env](#10-le-fichier-env)
11. [Workflow Git et Pull Requests](#11-workflow-git-et-pull-requests)
12. [Comment ajouter un nouveau service](#12-comment-ajouter-un-nouveau-service)
13. [Ports et conventions de nommage](#13-ports-et-conventions-de-nommage)
14. [Erreurs classiques à éviter](#14-erreurs-classiques-à-éviter)
15. [Checklist finale avant de partager le projet](#15-checklist-finale)

---

## 1. Comprendre l'architecture globale

### Pourquoi des microservices ?

Dans une architecture **monolithique** classique, tout le code est dans un seul projet :
- Si le module "Auth" tombe en panne → tout le système tombe
- Si tu veux mettre à jour le module "Mechanic" → tu redéploies TOUT
- Deux développeurs sur le même fichier = conflits permanents

Dans une architecture **microservices** :
- Chaque fonctionnalité est un service indépendant
- Un service peut tomber sans affecter les autres
- Chaque équipe travaille dans son propre dossier/repo
- Tu peux scaler (multiplier) uniquement le service qui est surchargé

### Le schéma de ton architecture

```
┌─────────────────────────────────────────────────────────┐
│                    FRONTEND (React)                      │
│              http://localhost:3000                        │
└───────────────────────┬─────────────────────────────────┘
                        │  Toutes les requêtes passent ici
                        ▼
┌─────────────────────────────────────────────────────────┐
│                  API GATEWAY                             │
│              http://localhost:8080                        │
│   /api/auth/**  ──►  auth-service                        │
│   /api/users/** ──►  user-service                        │
│   /api/mech/**  ──►  mechanic-service                    │
└───────────┬─────────────┬─────────────┬─────────────────┘
            │             │             │
            ▼             ▼             ▼
      ┌──────────┐  ┌──────────┐  ┌──────────────┐
      │  AUTH    │  │  USER    │  │   MECHANIC   │
      │ SERVICE  │  │ SERVICE  │  │   SERVICE    │
      │  :8081   │  │  :8082   │  │    :8083     │
      └──────┬───┘  └──────┬───┘  └──────┬───────┘
             │             │             │
             └─────────────┼─────────────┘
                           ▼
                  ┌─────────────────┐
                  │   POSTGRESQL    │
                  │    :5432        │
                  └─────────────────┘

       ╔═══════════════════════════════╗
       ║    EUREKA SERVER (Registre)   ║
       ║    http://localhost:8761       ║
       ║  Tous les services s'y        ║
       ║  enregistrent automatiquement ║
       ╚═══════════════════════════════╝
```

### Rôle de chaque composant

| Composant | Rôle | Pourquoi c'est important |
|-----------|------|--------------------------|
| **React Frontend** | Interface utilisateur | Parle UNIQUEMENT à la Gateway |
| **API Gateway** | Porte d'entrée unique | Redirige, sécurise, filtre les requêtes |
| **Eureka Server** | Registre des services | Les services se trouvent sans connaître les IPs/ports |
| **Auth Service** | Login, Register, JWT | Gère toute la sécurité |
| **User Service** | Profils utilisateurs | Données conducteurs/mécaniciens |
| **PostgreSQL** | Base de données | Persistance des données |

---

## 2. Créer le dépôt GitHub

### Pourquoi GitHub en premier ?

Avant d'écrire une seule ligne de code, le dépôt GitHub doit exister.
Raisons :
- Tout le code sera versionné dès le début (zéro risque de perdre du travail)
- Les autres développeurs peuvent cloner et contribuer immédiatement
- Tu peux forcer des règles (personne ne push directement sur `main`)

### Étape 1 — Créer le repo sur GitHub.com

1. Va sur [https://github.com](https://github.com)
2. Clique sur **"New repository"** (bouton vert en haut à droite)
3. Remplis :
   - **Repository name** : `car-assistance-platform`
   - **Description** : `Plateforme d'assistance automobile — Architecture microservices Spring Boot`
   - **Visibility** : `Public` (ou `Private` selon votre préférence)
   - ✅ Coche **"Add a README file"**
   - ✅ Coche **"Add .gitignore"** → choisis `Java`
4. Clique **"Create repository"**

### Étape 2 — Cloner le repo en local

```bash
# Remplace TON_USERNAME par ton pseudo GitHub
git clone https://github.com/TON_USERNAME/car-assistance-platform.git

# Entre dans le dossier
cd car-assistance-platform
```

### Étape 3 — Créer les branches obligatoires

```bash
# Tu es sur 'main' par défaut
# Crée la branche 'develop' à partir de main
git checkout -b develop

# Pousse develop sur GitHub
git push origin develop
```

### Étape 4 — Protéger la branche `main` sur GitHub

> **Pourquoi ?** Pour éviter que quelqu'un pousse directement sur `main` sans review.

1. Va dans ton repo → **Settings** → **Branches**
2. Clique **"Add branch protection rule"**
3. Dans **"Branch name pattern"** : `main`
4. Coche :
   - ✅ **Require a pull request before merging**
   - ✅ **Require approvals** (1 approbation minimum)
5. Clique **"Create"**

Répète pour la branche `develop`.

---

## 3. Créer la structure du projet

### Pourquoi cette structure ?

Une bonne structure de dossiers permet à n'importe quel développeur de comprendre le projet en 30 secondes. C'est ce qu'on appelle la **convention over configuration** : tout le monde suit les mêmes règles sans se poser de questions.

### Créer tous les dossiers en une commande

```bash
# Assure-toi d'être dans le dossier car-assistance-platform/
mkdir -p backend/discovery-service \
         backend/gateway-service \
         backend/auth-service \
         backend/user-service \
         frontend \
         infrastructure/docker \
         infrastructure/nginx \
         docs
```

### Structure finale attendue

```
car-assistance-platform/
│
├── backend/
│   ├── discovery-service/     ← Eureka Server (registre)
│   ├── gateway-service/       ← API Gateway (porte d'entrée)
│   ├── auth-service/          ← Authentification, JWT
│   └── user-service/          ← Profils utilisateurs
│
├── frontend/                  ← Application React
│
├── infrastructure/
│   ├── docker/                ← Dockerfiles partagés (si besoin)
│   └── nginx/                 ← Config reverse proxy (plus tard)
│
├── docs/                      ← Documentation technique
│
├── docker-compose.yml         ← Lance tout le système
├── .env                       ← Variables d'environnement
├── .gitignore
└── README.md
```

### Pousser la structure sur GitHub

```bash
# Crée un fichier .gitkeep pour que les dossiers vides soient trackés par git
find . -type d -empty -not -path "./.git/*" -exec touch {}/.gitkeep \;

# Ajoute tout
git add .

# Commit
git commit -m "chore: initialise la structure du projet microservices"

# Pousse sur develop (JAMAIS directement sur main)
git push origin develop
```

> **Pourquoi `chore:` devant le message de commit ?**
> C'est la convention **Conventional Commits**. Cela permet de comprendre d'un coup d'œil le type de changement :
> - `feat:` → nouvelle fonctionnalité
> - `fix:` → correction de bug
> - `chore:` → tâche de maintenance (config, structure)
> - `docs:` → documentation

---

## 4. Le Discovery Service (Eureka)

### Pourquoi Eureka ?

Sans Eureka, la Gateway doit connaître l'adresse exacte de chaque service :
```yaml
# SANS Eureka — Mauvaise approche
routes:
  - uri: http://localhost:8081  # Si ce port change → tout casse
```

Avec Eureka, chaque service s'enregistre sous un **nom logique** :
```yaml
# AVEC Eureka — Bonne approche
routes:
  - uri: lb://AUTH-SERVICE  # lb = load balancer via Eureka
```

`lb://AUTH-SERVICE` signifie : *"Va chercher dans Eureka le service qui s'appelle AUTH-SERVICE"*.
Peu importe son IP, peu importe son port.

### Créer le projet Spring Boot

#### Option A — Via Spring Initializr (recommandé)

1. Va sur [https://start.spring.io](https://start.spring.io)
2. Configure :
   - **Project** : `Maven`
   - **Language** : `Java`
   - **Spring Boot** : `3.2.x` (dernière stable)
   - **Group** : `com.mecano`
   - **Artifact** : `eureka-service`
   - **Name** : `eureka-service`
   - **Java** : `21`
3. Dépendances à ajouter :
   - ✅ **Eureka Server**
4. Clique **"Generate"** → extrait le ZIP dans `backend/`

### Le fichier `pom.xml` — Explication

```xml
<!-- backend/discovery-service/pom.xml -->
<dependencies>

    <!-- Eureka Server : transforme cette app en registre de services -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
    </dependency>

    <!-- Spring Boot Actuator : endpoints /health, /info pour monitorer le service -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

</dependencies>

<!-- Gestion des versions Spring Cloud — OBLIGATOIRE avec Spring Boot 3 -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>2023.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### La classe principale

```java
// backend/eureka-service/src/main/java/com/carplatform/discoveryservice/DiscoveryServiceApplication.java

package com.carplatform.discoveryservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer  // ← C'est CETTE annotation qui active le serveur Eureka
                     // Sans elle, c'est juste une app Spring Boot vide
public class DiscoveryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServiceApplication.class, args);
    }
}
```

> **Pourquoi `@EnableEurekaServer` ?**
> Cette annotation dit à Spring : *"Cette application EST le serveur Eureka.
> Commence à écouter les enregistrements des autres services."*

### Le fichier de configuration `application.yml`

```yaml
# backend/discovery-service/src/main/resources/application.yml

server:
  port: 8761  # Port standard d'Eureka — ne pas changer, c'est une convention

spring:
  application:
    name: DISCOVERY-SERVICE  # Nom en MAJUSCULES — convention du projet

eureka:
  instance:
    hostname: localhost  # En développement local
  client:
    # Ces deux lignes sont CRUCIALES et contre-intuitives :
    # Eureka NE doit PAS s'enregistrer lui-même (il est le registre, pas un client)
    register-with-eureka: false
    fetch-registry: false
  server:
    # Délai avant de retirer un service non-répondant (en ms)
    # 0 = immédiat (pratique en dev, à ajuster en prod)
    eviction-interval-timer-in-ms: 0

# Exposer les endpoints de santé (utile pour Docker healthcheck)
management:
  endpoints:
    web:
      exposure:
        include: health,info
```

> **Pourquoi `register-with-eureka: false` ?**
> Par défaut, TOUT service Spring Cloud essaie de s'enregistrer dans Eureka.
> Mais Eureka lui-même ne peut pas s'enregistrer chez... lui-même.
> Ces deux lignes désactivent ce comportement pour le serveur.

### Tester le Discovery Service

```bash
# Dans backend/discovery-service/
mvn spring-boot:run

# Puis ouvre ton navigateur sur :
# http://localhost:8761
# Tu verras le dashboard Eureka avec "No instances currently registered"
```

---

## 5. Le Gateway Service

### Pourquoi une API Gateway ?

Imagine que le Frontend parle directement à tous les services :
```
React → http://localhost:8081/login       ← Auth Service
React → http://localhost:8082/users/1     ← User Service
React → http://localhost:8083/mechanics   ← Mechanic Service
```

Problèmes :
- Le Frontend doit connaître l'adresse de TOUS les services
- Si un port change → le frontend casse
- Pas de point unique pour ajouter de la sécurité, des logs, du rate limiting

Avec une Gateway :
```
React → http://localhost:8080/api/auth/login       ← Gateway redirige vers Auth
React → http://localhost:8080/api/users/1          ← Gateway redirige vers User
React → http://localhost:8080/api/mechanics        ← Gateway redirige vers Mechanic
```

La Gateway est le **seul** interlocuteur du Frontend.

### Créer le projet

Sur [https://start.spring.io](https://start.spring.io) :
- **Artifact** : `gateway-service`
- Dépendances :
  - ✅ **Gateway** (Spring Cloud Gateway)
  - ✅ **Eureka Discovery Client**
  - ✅ **Actuator**

> **⚠️ Important** : Spring Cloud Gateway est basé sur **WebFlux** (réactif).
> N'ajoute PAS `Spring Web` (MVC) — ils sont incompatibles !

### La classe principale

```java
// backend/gateway-service/src/main/java/com/carplatform/gatewayservice/GatewayServiceApplication.java

package com.carplatform.gatewayservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient  // ← Dit à Spring : "cette app est un CLIENT Eureka"
                        // Elle va s'enregistrer dans Eureka ET l'utiliser
public class GatewayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }
}
```

### Le fichier de configuration `application.yml`

```yaml
# backend/gateway-service/src/main/resources/application.yml

server:
  port: 8080  # Port unique d'entrée pour tout le système

spring:
  application:
    name: GATEWAY-SERVICE  # Nom que verra Eureka

  cloud:
    gateway:
      # Active la découverte automatique via Eureka
      discovery:
        locator:
          enabled: true
          lower-case-service-id: true  # auth-service au lieu de AUTH-SERVICE dans les URLs

      # Définition manuelle des routes (plus de contrôle)
      routes:

        # ─── ROUTE 1 : Auth Service ───────────────────────────────────────────
        - id: auth-service-route        # Identifiant unique de la route
          uri: lb://AUTH-SERVICE        # lb:// = Load Balanced via Eureka
                                        # AUTH-SERVICE = nom dans Eureka
          predicates:
            - Path=/api/auth/**         # Toute requête commençant par /api/auth/
                                        # est redirigée vers AUTH-SERVICE
          filters:
            - StripPrefix=1             # Supprime /api du chemin avant de transmettre
                                        # /api/auth/login → /auth/login côté service

        # ─── ROUTE 2 : User Service ───────────────────────────────────────────
        - id: user-service-route
          uri: lb://USER-SERVICE
          predicates:
            - Path=/api/users/**
          filters:
            - StripPrefix=1

        # ─── ROUTE 3 : Mechanic Service ───────────────────────────────────────
        - id: mechanic-service-route
          uri: lb://MECHANIC-SERVICE
          predicates:
            - Path=/api/mechanics/**
          filters:
            - StripPrefix=1

      # Configuration CORS (Cross-Origin Resource Sharing)
      # Nécessaire pour que React (port 3000) puisse appeler la Gateway (port 8080)
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOrigins:
              - "http://localhost:3000"  # Adresse du frontend React
            allowedMethods:
              - GET
              - POST
              - PUT
              - DELETE
              - OPTIONS
            allowedHeaders: "*"
            allowCredentials: true

eureka:
  client:
    service-url:
      # Adresse du serveur Eureka
      # En local : localhost:8761
      # En Docker : le nom du conteneur (discovery-service)
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true  # Utilise l'IP plutôt que le hostname (mieux pour Docker)

management:
  endpoints:
    web:
      exposure:
        include: health,info,gateway
```

> **Explication du `StripPrefix=1`** :
>
> Quand React envoie `GET /api/auth/login`,
> la Gateway reçoit `/api/auth/login`.
> Sans StripPrefix, elle transmettrait `/api/auth/login` à l'Auth Service.
> Avec `StripPrefix=1`, elle supprime le premier segment `/api` et transmet `/auth/login`.
> L'Auth Service n'a donc pas besoin de gérer le préfixe `/api`.

### Vérifier que la Gateway voit Eureka

```bash
# Lance d'abord Eureka (dans un terminal 1)
cd backend/discovery-service && mvn spring-boot:run

# Lance la Gateway (dans un terminal 2)
cd backend/gateway-service && mvn spring-boot:run

# Ouvre http://localhost:8761
# Tu dois voir GATEWAY-SERVICE dans la liste "Instances currently registered"
```

---

## 6. L'Auth Service

### Responsabilités

L'Auth Service gère **exclusivement** :
- Inscription (`POST /auth/register`)
- Connexion (`POST /auth/login`)
- Génération de tokens JWT
- Refresh de tokens
- Validation de tokens (pour les autres services)

Il NE gère PAS les profils utilisateurs → c'est le rôle du User Service.

### Créer le projet

Sur [https://start.spring.io](https://start.spring.io) :
- **Artifact** : `auth-service`
- Dépendances :
  - ✅ **Spring Web**
  - ✅ **Spring Security**
  - ✅ **Spring Data JPA**
  - ✅ **PostgreSQL Driver**
  - ✅ **Eureka Discovery Client**
  - ✅ **Validation**
  - ✅ **Actuator**
  - ✅ **Lombok** (optionnel mais pratique)

### Ajouter JWT dans `pom.xml`

```xml
<!-- Librairie JWT — ajoute ceci dans <dependencies> -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
```

### Structure interne du service

```
auth-service/src/main/java/com/carplatform/authservice/
│
├── AuthServiceApplication.java       ← Classe principale
│
├── config/
│   └── SecurityConfig.java           ← Configuration Spring Security
│
├── controller/
│   └── AuthController.java           ← Endpoints REST (/auth/login, /auth/register)
│
├── dto/
│   ├── LoginRequest.java             ← Données reçues pour le login
│   ├── RegisterRequest.java          ← Données reçues pour l'inscription
│   └── AuthResponse.java             ← Réponse avec le token JWT
│
├── entity/
│   └── User.java                     ← Entité JPA (table users en base)
│
├── repository/
│   └── UserRepository.java           ← Interface Spring Data JPA
│
└── service/
    ├── AuthService.java              ← Logique métier (login, register)
    └── JwtService.java               ← Génération et validation des tokens
```

> **Pourquoi cette structure ?**
> C'est le pattern **Controller → Service → Repository** :
> - Le Controller reçoit la requête HTTP et retourne une réponse
> - Le Service contient la logique métier (ce qu'on fait réellement)
> - Le Repository parle à la base de données
> Chaque couche a UNE responsabilité → plus facile à tester, à modifier

### La classe principale

```java
// AuthServiceApplication.java
package com.carplatform.authservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient  // S'enregistre dans Eureka
public class AuthServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
```

### L'entité User

```java
// entity/User.java
package com.carplatform.authservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "users")          // Nom de la table en base de données
@Data                           // Lombok génère getters, setters, toString, equals, hashCode
@NoArgsConstructor              // Lombok génère un constructeur sans argument
@AllArgsConstructor             // Lombok génère un constructeur avec tous les arguments
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // Auto-increment
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;    // Sera hashé avec BCrypt — JAMAIS en clair

    @Enumerated(EnumType.STRING)  // Stocke le nom du rôle (ex: "DRIVER") pas un nombre
    private Role role;

    public enum Role {
        DRIVER,     // Conducteur
        MECHANIC,   // Mécanicien
        ADMIN       // Administrateur
    }
}
```

### Le Repository

```java
// repository/UserRepository.java
package com.carplatform.authservice.repository;

import com.carplatform.authservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

// JpaRepository<User, Long> :
// - User = l'entité gérée
// - Long = le type de la clé primaire
// Spring génère automatiquement : save(), findById(), findAll(), delete()...
public interface UserRepository extends JpaRepository<User, Long> {

    // Spring génère automatiquement la requête SQL :
    // SELECT * FROM users WHERE email = ?
    Optional<User> findByEmail(String email);

    // Vérifie si un email existe déjà
    boolean existsByEmail(String email);
}
```

### Les DTOs (Data Transfer Objects)

```java
// dto/RegisterRequest.java
// DTO = objet qui transporte les données entre le client et le serveur
// Il ne contient PAS de logique métier — juste des champs et validations
package com.carplatform.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @Email(message = "Format email invalide")
    @NotBlank(message = "L'email est obligatoire")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    private String password;

    private String role;  // "DRIVER", "MECHANIC", "ADMIN"
}
```

```java
// dto/LoginRequest.java
package com.carplatform.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;
}
```

```java
// dto/AuthResponse.java
package com.carplatform.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;           // JWT Access Token
    private String refreshToken;    // Refresh Token (pour renouveler le token)
    private String role;            // Rôle de l'utilisateur
    private Long userId;            // ID de l'utilisateur
}
```

### Le Service JWT

```java
// service/JwtService.java
package com.carplatform.authservice.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    // Clé secrète lue depuis application.yml (pas hardcodée dans le code !)
    @Value("${jwt.secret}")
    private String secretKey;

    // Durée de validité du token (24h en millisecondes)
    @Value("${jwt.expiration}")
    private long expiration;

    // Génère un token JWT pour un utilisateur
    public String generateToken(String email, String role, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("userId", userId);

        return Jwts.builder()
                .setClaims(claims)              // Données dans le token
                .setSubject(email)              // Identifiant principal
                .setIssuedAt(new Date())        // Date de création
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)  // Signature
                .compact();                     // Génère la chaîne JWT
    }

    // Extrait l'email du token
    public String extractEmail(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    // Vérifie si le token est encore valide
    public boolean isTokenValid(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSignKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            return false;  // Token expiré ou invalide
        }
    }

    private Key getSignKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }
}
```

### Le Service Auth

```java
// service/AuthService.java
package com.carplatform.authservice.service;

import com.carplatform.authservice.dto.*;
import com.carplatform.authservice.entity.User;
import com.carplatform.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor  // Lombok génère un constructeur avec tous les champs final
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;  // BCrypt pour hasher les mots de passe
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {
        // Vérifier si l'email existe déjà
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email déjà utilisé");
        }

        // Créer l'utilisateur
        User user = new User();
        user.setEmail(request.getEmail());
        // IMPORTANT : On ne stocke JAMAIS le mot de passe en clair
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(User.Role.valueOf(request.getRole().toUpperCase()));

        // Sauvegarder en base
        User savedUser = userRepository.save(user);

        // Générer le token JWT
        String token = jwtService.generateToken(
                savedUser.getEmail(),
                savedUser.getRole().name(),
                savedUser.getId()
        );

        return new AuthResponse(token, null, savedUser.getRole().name(), savedUser.getId());
    }

    public AuthResponse login(LoginRequest request) {
        // Chercher l'utilisateur par email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Vérifier le mot de passe
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Mot de passe incorrect");
        }

        // Générer le token
        String token = jwtService.generateToken(
                user.getEmail(),
                user.getRole().name(),
                user.getId()
        );

        return new AuthResponse(token, null, user.getRole().name(), user.getId());
    }
}
```

### Le Controller

```java
// controller/AuthController.java
package com.carplatform.authservice.controller;

import com.carplatform.authservice.dto.*;
import com.carplatform.authservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")   // Toutes les routes commencent par /auth
                           // Gateway transmet /api/auth/login → /auth/login ici
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        // @Valid déclenche les validations des annotations @NotBlank, @Email, etc.
        // @RequestBody convertit le JSON reçu en objet Java
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // Endpoint de santé simple (utile pour les tests)
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Auth Service is running ✅");
    }
}
```

### La configuration Spring Security

```java
// config/SecurityConfig.java
package com.carplatform.authservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())  // Désactive CSRF (API REST = stateless)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                // STATELESS = pas de session côté serveur, JWT gère tout
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**").permitAll()    // Login/Register = public
                .requestMatchers("/actuator/**").permitAll() // Health checks = public
                .anyRequest().authenticated()               // Tout le reste = authentifié
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt est un algorithme de hashage sécurisé (avec sel automatique)
        return new BCryptPasswordEncoder();
    }
}
```

### Le fichier `application.yml` de l'Auth Service

```yaml
# backend/auth-service/src/main/resources/application.yml

server:
  port: 8081

spring:
  application:
    name: AUTH-SERVICE  # Nom visible dans Eureka — en MAJUSCULES par convention

  # Configuration de la base de données PostgreSQL
  datasource:
    url: jdbc:postgresql://localhost:5432/auth_db  # En Docker : postgres:5432
    username: ${DB_USERNAME:postgres}   # Valeur par défaut si la variable n'est pas définie
    password: ${DB_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver

  # Configuration JPA/Hibernate
  jpa:
    hibernate:
      ddl-auto: update   # "update" = crée les tables si elles n'existent pas, met à jour sinon
                         # En production → utiliser "validate" et gérer les migrations avec Flyway
    show-sql: false      # true = affiche les requêtes SQL dans la console (utile en debug)
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

# Configuration JWT — les valeurs viennent du fichier .env via Docker
jwt:
  secret: ${JWT_SECRET:changeMeInProduction64CharactersMinimumForSecurity}
  expiration: 86400000  # 24 heures en millisecondes

# Connexion à Eureka
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true

# Endpoints Actuator
management:
  endpoints:
    web:
      exposure:
        include: health,info
```

---

## 7. Le User Service

### Responsabilités

Le User Service gère :
- Les profils complets des utilisateurs (nom, photo, coordonnées)
- Les informations spécifiques aux conducteurs et mécaniciens
- La récupération et mise à jour des profils

> **Note** : Il ne gère PAS les mots de passe ni les tokens — c'est l'Auth Service.

### Structure interne

```
user-service/src/main/java/com/carplatform/userservice/
│
├── UserServiceApplication.java
│
├── controller/
│   └── UserController.java
│
├── dto/
│   ├── UserProfileDto.java
│   └── UpdateProfileRequest.java
│
├── entity/
│   └── UserProfile.java
│
├── repository/
│   └── UserProfileRepository.java
│
└── service/
    └── UserService.java
```

### La classe principale

```java
// UserServiceApplication.java
package com.carplatform.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
```

### L'entité UserProfile

```java
// entity/UserProfile.java
package com.carplatform.userservice.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "user_profiles")
@Data
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // authUserId = l'ID de l'utilisateur dans auth-service
    // On relie les deux services par cet ID, sans faire de clé étrangère inter-services
    // C'est une règle FONDAMENTALE des microservices :
    // chaque service a sa propre base, on ne fait pas de JOIN entre services
    @Column(unique = true, nullable = false)
    private Long authUserId;

    private String firstName;
    private String lastName;
    private String phone;
    private String address;
    private String profilePicture;  // URL de la photo de profil

    @Enumerated(EnumType.STRING)
    private UserType userType;

    public enum UserType {
        DRIVER, MECHANIC, ADMIN
    }
}
```

### Le fichier `application.yml`

```yaml
# backend/user-service/src/main/resources/application.yml

server:
  port: 8082

spring:
  application:
    name: USER-SERVICE

  datasource:
    url: jdbc:postgresql://localhost:5432/user_db  # Base SÉPARÉE de auth_db !
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true

management:
  endpoints:
    web:
      exposure:
        include: health,info
```

> **Pourquoi `user_db` et non `auth_db` ?**
> Règle d'or des microservices : **chaque service possède sa propre base de données**.
> Ils ne partagent jamais une même base. Si tu as besoin de données d'un autre service,
> tu fais un appel HTTP à ce service. Jamais un JOIN SQL direct.

---

## 8. Dockeriser chaque service

### Pourquoi Docker ?

Sans Docker :
- Développeur 1 a Java 17, Développeur 2 a Java 21 → comportements différents
- "Ça marche chez moi" est la phrase la plus dangereuse en développement
- Le déploiement en production demande une configuration manuelle complexe

Avec Docker :
- Chaque service tourne dans un **conteneur isolé** avec son propre environnement
- L'image Docker est **identique** en local, en CI/CD et en production
- Une commande (`docker compose up`) lance tout le système

### Le Dockerfile du Discovery Service

```dockerfile
# backend/discovery-service/Dockerfile

# ── ÉTAPE 1 : Build ────────────────────────────────────────────────────────────
# On utilise une image Maven+JDK pour compiler le code
FROM maven:3.9-eclipse-temurin-21 AS build
# AS build = on nomme cette étape "build" pour y faire référence après

WORKDIR /app
# Copie d'abord le pom.xml pour profiter du cache Docker
# Docker met en cache les couches : si pom.xml n'a pas changé,
# il n'a pas besoin de re-télécharger les dépendances
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copie le code source
COPY src ./src

# Compile et package (sans les tests pour accélérer)
RUN mvn clean package -DskipTests

# ── ÉTAPE 2 : Runtime ──────────────────────────────────────────────────────────
# Image finale bien plus légère (JRE uniquement, sans Maven)
FROM eclipse-temurin:21-jre-alpine
# alpine = version ultra-légère de Linux (~5MB vs ~200MB)

WORKDIR /app

# Copie seulement le JAR compilé depuis l'étape "build"
COPY --from=build /app/target/*.jar app.jar

# Expose le port du service
EXPOSE 8761

# Commande de démarrage
ENTRYPOINT ["java", "-jar", "app.jar"]
```

> **Pourquoi le multi-stage build ?**
> L'image finale ne contient que le JRE et le JAR. Pas Maven, pas les sources, pas les dépendances source.
> Résultat : image ~100-150MB au lieu de ~500MB.

### Le Dockerfile du Gateway Service

```dockerfile
# backend/gateway-service/Dockerfile

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Le Dockerfile de l'Auth Service

```dockerfile
# backend/auth-service/Dockerfile

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Le Dockerfile du User Service

```dockerfile
# backend/user-service/Dockerfile

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 9. Le docker-compose.yml global

### Pourquoi docker-compose ?

`docker-compose.yml` est le **chef d'orchestre**. Il définit :
- Quels services lancer
- Dans quel ordre
- Comment ils communiquent entre eux
- Quelles variables d'environnement utiliser

**Une seule commande pour tout lancer :**
```bash
docker compose up --build
```

### Le fichier complet avec explications

```yaml
# docker-compose.yml (à la racine du projet)

version: '3.9'

# ── RÉSEAU ─────────────────────────────────────────────────────────────────────
# Tous les services partagent ce réseau Docker
# Cela leur permet de se parler par nom de service (ex: discovery-service:8761)
# au lieu d'adresses IP qui changent
networks:
  car-platform-network:
    driver: bridge

# ── VOLUMES ────────────────────────────────────────────────────────────────────
# Volume persistant pour PostgreSQL
# Sans volume, les données sont perdues quand le conteneur s'arrête !
volumes:
  postgres-data:

# ── SERVICES ───────────────────────────────────────────────────────────────────
services:

  # ────────────────────────────────────────────────────────────────────────────
  # 1. PostgreSQL — Lance EN PREMIER (les services en dépendent)
  # ────────────────────────────────────────────────────────────────────────────
  postgres:
    image: postgres:16-alpine           # Version officielle, légère
    container_name: car-postgres
    environment:
      POSTGRES_USER: ${DB_USERNAME}     # Vient du fichier .env
      POSTGRES_PASSWORD: ${DB_PASSWORD}
      POSTGRES_MULTIPLE_DATABASES: auth_db,user_db  # Crée plusieurs bases
    volumes:
      - postgres-data:/var/lib/postgresql/data  # Persistance des données
      - ./infrastructure/docker/init-db.sh:/docker-entrypoint-initdb.d/init-db.sh
      # Script d'initialisation pour créer plusieurs bases de données
    ports:
      - "5432:5432"   # host:conteneur — pour accès depuis ton IDE/pgAdmin
    networks:
      - car-platform-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USERNAME}"]
      interval: 10s
      timeout: 5s
      retries: 5
      # Le healthcheck permet aux autres services de savoir quand Postgres est PRÊT

  # ────────────────────────────────────────────────────────────────────────────
  # 2. Discovery Service (Eureka) — Lance EN DEUXIÈME
  # ────────────────────────────────────────────────────────────────────────────
  discovery-service:
    build:
      context: ./backend/discovery-service  # Dossier contenant le Dockerfile
      dockerfile: Dockerfile
    container_name: car-discovery
    ports:
      - "8761:8761"
    networks:
      - car-platform-network
    healthcheck:
      test: ["CMD-SHELL", "wget -q --spider http://localhost:8761/actuator/health || exit 1"]
      interval: 15s
      timeout: 10s
      retries: 5

  # ────────────────────────────────────────────────────────────────────────────
  # 3. Gateway Service — Lance APRÈS Eureka
  # ────────────────────────────────────────────────────────────────────────────
  gateway-service:
    build:
      context: ./backend/gateway-service
      dockerfile: Dockerfile
    container_name: car-gateway
    ports:
      - "8080:8080"
    environment:
      # En Docker, on utilise le nom du service (discovery-service) au lieu de localhost
      # Car localhost dans un conteneur = LE conteneur lui-même, pas l'hôte
      EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE: http://discovery-service:8761/eureka/
    networks:
      - car-platform-network
    depends_on:
      discovery-service:
        condition: service_healthy   # Attend qu'Eureka soit SAIN avant de démarrer
    restart: on-failure              # Redémarre si ça plante (pendant l'init d'Eureka)

  # ────────────────────────────────────────────────────────────────────────────
  # 4. Auth Service — Lance APRÈS Eureka et Postgres
  # ────────────────────────────────────────────────────────────────────────────
  auth-service:
    build:
      context: ./backend/auth-service
      dockerfile: Dockerfile
    container_name: car-auth
    ports:
      - "8081:8081"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/auth_db
      # "postgres" = nom du service PostgreSQL dans docker-compose
      # En dehors de Docker, c'était "localhost"
      SPRING_DATASOURCE_USERNAME: ${DB_USERNAME}
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE: http://discovery-service:8761/eureka/
      JWT_SECRET: ${JWT_SECRET}
    networks:
      - car-platform-network
    depends_on:
      postgres:
        condition: service_healthy
      discovery-service:
        condition: service_healthy
    restart: on-failure

  # ────────────────────────────────────────────────────────────────────────────
  # 5. User Service
  # ────────────────────────────────────────────────────────────────────────────
  user-service:
    build:
      context: ./backend/user-service
      dockerfile: Dockerfile
    container_name: car-user
    ports:
      - "8082:8082"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/user_db
      SPRING_DATASOURCE_USERNAME: ${DB_USERNAME}
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE: http://discovery-service:8761/eureka/
    networks:
      - car-platform-network
    depends_on:
      postgres:
        condition: service_healthy
      discovery-service:
        condition: service_healthy
    restart: on-failure
```

### Script d'initialisation des bases PostgreSQL

```bash
# infrastructure/docker/init-db.sh
#!/bin/bash

# Ce script est exécuté automatiquement par PostgreSQL au premier démarrage
# Il crée les bases de données pour chaque service

set -e  # Arrêter le script si une commande échoue

# Créer la base auth_db
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE DATABASE auth_db;
    GRANT ALL PRIVILEGES ON DATABASE auth_db TO $POSTGRES_USER;
EOSQL

echo "✅ Base auth_db créée"

# Créer la base user_db
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE DATABASE user_db;
    GRANT ALL PRIVILEGES ON DATABASE user_db TO $POSTGRES_USER;
EOSQL

echo "✅ Base user_db créée"
```

```bash
# Rendre le script exécutable
chmod +x infrastructure/docker/init-db.sh
```

---

## 10. Le fichier .env

### Pourquoi un fichier .env ?

Les secrets (mots de passe, clés JWT...) ne doivent **JAMAIS** être dans le code source.
Si tu pushs un mot de passe sur GitHub → problème de sécurité permanent (même supprimé, l'historique Git le garde).

Le fichier `.env` contient les valeurs sensibles et est **ignoré par Git**.

### Le fichier `.env`

```bash
# .env — Variables d'environnement
# ⚠️ CE FICHIER N'EST JAMAIS COMMITÉ SUR GITHUB ⚠️

# ─── Base de données ───────────────────────────────────────────────────────────
DB_USERNAME=postgres
DB_PASSWORD=CarPlatform2024!   # Utilise un mot de passe fort en production

# ─── JWT ───────────────────────────────────────────────────────────────────────
# La clé doit faire au minimum 64 caractères pour HS256
JWT_SECRET=votreCleJwtTresLongueEtComplexePourLaSecuriteDeVotreApplication2024Minimum64Chars

# ─── Profils Spring ────────────────────────────────────────────────────────────
SPRING_PROFILES_ACTIVE=docker   # Active le profil "docker" dans Spring Boot
```

### Le fichier `.env.example`

```bash
# .env.example — Template pour les nouveaux développeurs
# Copie ce fichier en .env et remplis les valeurs réelles
# Ce fichier EST commité sur GitHub (pas de secrets ici)

DB_USERNAME=
DB_PASSWORD=
JWT_SECRET=
SPRING_PROFILES_ACTIVE=docker
```

### Mettre à jour `.gitignore`

```gitignore
# .gitignore

# ─── Java / Maven ───────────────────────────────────────────────────────────
target/
*.class
*.jar
*.war

# ─── Spring Boot ────────────────────────────────────────────────────────────
*.log
logs/

# ─── IDE ────────────────────────────────────────────────────────────────────
.idea/
*.iml
.vscode/
.eclipse/

# ─── Docker ─────────────────────────────────────────────────────────────────
# Pas de .dockerignore global ici (dans chaque service)

# ─── Environnement ──────────────────────────────────────────────────────────
.env              # ← JAMAIS sur GitHub !
.env.local
.env.*.local

# ─── Node / React ───────────────────────────────────────────────────────────
node_modules/
frontend/dist/
frontend/build/

# ─── OS ─────────────────────────────────────────────────────────────────────
.DS_Store
Thumbs.db
```

---

## 11. Workflow Git et Pull Requests

### Le modèle de branches

```
main          ──────────────────────────────── Production (protégée)
                            ▲
develop       ──────────────┼──────────────── Développement (intégration)
               ▲            ▲
feature/...   ─┘  feature/... ┘               Fonctionnalités individuelles
```

### Règles absolues

| Branche | Qui peut pousser ? | Quand est-elle mise à jour ? |
|---------|-------------------|------------------------------|
| `main` | Personne directement | Uniquement via PR depuis `develop` |
| `develop` | Personne directement | Uniquement via PR depuis les features |
| `feature/*` | Le développeur concerné | Quand il travaille |

### Workflow complet pour un développeur

```bash
# ── 1. Toujours partir d'un develop à jour ────────────────────────────────────
git checkout develop
git pull origin develop        # Récupère les dernières modifications

# ── 2. Créer sa branche de feature ───────────────────────────────────────────
git checkout -b feature/auth-login
# Convention : feature/QUOI-TU-FAIS (en anglais, avec des tirets)
# Exemples :
# feature/auth-register
# feature/user-profile-update
# fix/jwt-expiration-bug
# chore/update-docker-compose

# ── 3. Travailler et commiter régulièrement ───────────────────────────────────
git add .
git commit -m "feat(auth): ajoute l'endpoint de login avec JWT"
# Convention : type(scope): description courte en présent
# Types : feat, fix, chore, docs, test, refactor, style

# ── 4. Pousser la branche ─────────────────────────────────────────────────────
git push origin feature/auth-login

# ── 5. Créer une Pull Request sur GitHub ─────────────────────────────────────
# Va sur GitHub → ton repo → "Compare & pull request"
# Base : develop (pas main !)
# Compare : feature/auth-login
# Ajoute une description claire de ce que tu as fait

# ── 6. (Tech Lead) Review et merge ────────────────────────────────────────────
# Le Tech Lead vérifie le code, teste Docker, puis merge dans develop
```

### Conventions de commit (Conventional Commits)

```
feat(auth): ajoute login avec JWT
│    │      └── Description courte (présent, minuscule)
│    └── Scope (module concerné)
└── Type

Types :
  feat     → nouvelle fonctionnalité
  fix      → correction de bug
  chore    → maintenance, dépendances
  docs     → documentation uniquement
  test     → ajout/modification de tests
  refactor → refactoring sans changement de comportement
  style    → formatage, espaces (pas de logique)
  perf     → amélioration des performances
```

---

## 12. Comment ajouter un nouveau service

### Exemple : Ajouter le Mechanic Service

#### Étape 1 — Créer la branche

```bash
git checkout develop
git pull origin develop
git checkout -b feature/mechanic-service
```

#### Étape 2 — Créer le projet Spring Boot

Sur [https://start.spring.io](https://start.spring.io) :
- Artifact : `mechanic-service`
- Dépendances : Spring Web, Spring Data JPA, PostgreSQL Driver, Eureka Discovery Client, Actuator, Lombok
- Extraire dans `backend/mechanic-service/`

#### Étape 3 — Modifier `application.yml`

```yaml
server:
  port: 8083  # Prochain port disponible

spring:
  application:
    name: MECHANIC-SERVICE  # Nom en majuscules

  datasource:
    url: jdbc:postgresql://localhost:5432/mechanic_db
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

#### Étape 4 — Ajouter le Dockerfile

```dockerfile
# backend/mechanic-service/Dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### Étape 5 — Ajouter au docker-compose.yml

```yaml
# Dans docker-compose.yml, ajouter dans la section services :

  mechanic-service:
    build:
      context: ./backend/mechanic-service
      dockerfile: Dockerfile
    container_name: car-mechanic
    ports:
      - "8083:8083"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/mechanic_db
      SPRING_DATASOURCE_USERNAME: ${DB_USERNAME}
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE: http://discovery-service:8761/eureka/
    networks:
      - car-platform-network
    depends_on:
      postgres:
        condition: service_healthy
      discovery-service:
        condition: service_healthy
    restart: on-failure
```

#### Étape 6 — Ajouter la route dans la Gateway

```yaml
# Dans gateway-service/application.yml, ajouter dans spring.cloud.gateway.routes :

        - id: mechanic-service-route
          uri: lb://MECHANIC-SERVICE
          predicates:
            - Path=/api/mechanics/**
          filters:
            - StripPrefix=1
```

#### Étape 7 — Ajouter la base dans init-db.sh

```bash
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE DATABASE mechanic_db;
    GRANT ALL PRIVILEGES ON DATABASE mechanic_db TO $POSTGRES_USER;
EOSQL
echo "✅ Base mechanic_db créée"
```

#### Étape 8 — Vérifier dans Eureka

```bash
docker compose up --build mechanic-service

# Ouvre http://localhost:8761
# Tu dois voir MECHANIC-SERVICE dans la liste
```

#### Étape 9 — PR et merge

```bash
git add .
git commit -m "feat(mechanic): initialise le mechanic service"
git push origin feature/mechanic-service
# Créer la PR sur GitHub → develop
```

---

## 13. Ports et conventions de nommage

### Table des ports

| Service | Port | Container Name |
|---------|------|----------------|
| Eureka Discovery | 8761 | car-discovery |
| API Gateway | 8080 | car-gateway |
| Auth Service | 8081 | car-auth |
| User Service | 8082 | car-user |
| Mechanic Service | 8083 | car-mechanic |
| Notification Service | 8084 | car-notification |
| PostgreSQL | 5432 | car-postgres |
| React Frontend | 3000 | car-frontend |

### Noms des services dans Eureka

Toujours en **MAJUSCULES** avec des **tirets** :
```
DISCOVERY-SERVICE
GATEWAY-SERVICE
AUTH-SERVICE
USER-SERVICE
MECHANIC-SERVICE
```

### Noms des bases de données

Toujours `nom_service_db` :
```
auth_db
user_db
mechanic_db
notification_db
```

### Noms des endpoints

Pattern : `/api/{service}/{ressource}`
```
/api/auth/login
/api/auth/register
/api/users/{id}
/api/users/{id}/profile
/api/mechanics
/api/mechanics/{id}
```

---

## 14. Erreurs classiques à éviter

### ❌ Erreur 1 — Appeler les services par leur port directement

```yaml
# MAUVAIS
uri: http://localhost:8081

# BON
uri: lb://AUTH-SERVICE
```

**Pourquoi ?** Si le service change de port ou d'adresse, tout casse.
Avec `lb://`, Eureka gère l'adresse dynamiquement.

### ❌ Erreur 2 — Partager une base de données entre services

```
# MAUVAIS
auth-service → auth_db
user-service → auth_db  ← Interdit !

# BON
auth-service → auth_db
user-service → user_db  ← Chaque service a SA base
```

### ❌ Erreur 3 — Hardcoder les secrets dans le code

```java
// MAUVAIS
String secret = "monMotDePasse123";

// BON
@Value("${jwt.secret}")
String secret;
```

### ❌ Erreur 4 — Ne pas utiliser `depends_on` dans docker-compose

```yaml
# MAUVAIS
auth-service:
  # Lance en même temps que postgres — peut crasher si postgres n'est pas prêt

# BON
auth-service:
  depends_on:
    postgres:
      condition: service_healthy  # Attend que postgres soit prêt
```

### ❌ Erreur 5 — Utiliser `localhost` dans Docker

```yaml
# MAUVAIS (dans docker-compose)
SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/auth_db
# localhost dans un conteneur = le conteneur lui-même, pas l'hôte !

# BON
SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/auth_db
# "postgres" = nom du service dans docker-compose
```

### ❌ Erreur 6 — Pousser directement sur `main` ou `develop`

```bash
# MAUVAIS
git checkout main
git push  # Catastrophique en équipe

# BON
git checkout -b feature/ma-feature
git push origin feature/ma-feature
# Puis Pull Request
```

---

## 15. Checklist finale

Avant de partager le projet avec ton équipe, vérifie chaque point :

### Infrastructure
- [ ] Repository GitHub créé avec branches `main` et `develop`
- [ ] Branches `main` et `develop` protégées (pas de push direct)
- [ ] `.env` dans `.gitignore` et `.env.example` créé
- [ ] Structure de dossiers complète créée

### Discovery Service
- [ ] Projet Spring Boot créé avec dépendance Eureka Server
- [ ] `@EnableEurekaServer` présent
- [ ] `register-with-eureka: false` et `fetch-registry: false`
- [ ] Accessible sur `http://localhost:8761`
- [ ] Dockerfile créé et testé

### Gateway Service
- [ ] Projet Spring Boot créé (WebFlux, pas Spring MVC)
- [ ] Routes configurées pour chaque service
- [ ] CORS configuré pour React (`localhost:3000`)
- [ ] S'enregistre dans Eureka (visible sur `http://localhost:8761`)
- [ ] Dockerfile créé et testé

### Auth Service
- [ ] Login et Register fonctionnels
- [ ] JWT généré et validé
- [ ] Mots de passe hashés avec BCrypt
- [ ] Base `auth_db` créée séparément
- [ ] S'enregistre dans Eureka
- [ ] Dockerfile créé et testé

### User Service
- [ ] Profil utilisateur CRUD fonctionnel
- [ ] Base `user_db` séparée de `auth_db`
- [ ] S'enregistre dans Eureka
- [ ] Dockerfile créé et testé

### Docker Compose
- [ ] Tous les services définis
- [ ] `depends_on` avec `condition: service_healthy`
- [ ] Variables d'environnement depuis `.env`
- [ ] Volume PostgreSQL persistant
- [ ] Script `init-db.sh` crée toutes les bases
- [ ] `docker compose up --build` lance tout sans erreur

### Tests de bout en bout
```bash
# 1. Lance tout le système
docker compose up --build

# 2. Vérifie Eureka
# → http://localhost:8761 : tous les services visibles ?

# 3. Teste l'inscription via Gateway
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email": "test@test.com", "password": "password123", "role": "DRIVER"}'

# 4. Teste le login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "test@test.com", "password": "password123"}'
# → Doit retourner un token JWT

# 5. Arrête tout proprement
docker compose down
```

---

## Commandes utiles

```bash
# ── Docker ────────────────────────────────────────────────────────────────────

# Lancer tout le système (rebuild les images)
docker compose up --build

# Lancer en arrière-plan (daemon mode)
docker compose up -d --build

# Voir les logs en temps réel
docker compose logs -f

# Voir les logs d'un service spécifique
docker compose logs -f auth-service

# Arrêter tout (conserve les volumes)
docker compose down

# Arrêter et supprimer les volumes (repart de zéro)
docker compose down -v

# Voir les conteneurs qui tournent
docker compose ps

# ── Git ───────────────────────────────────────────────────────────────────────

# Voir toutes les branches
git branch -a

# Mettre à jour sa branche avec develop
git checkout feature/ma-feature
git rebase develop  # ou : git merge develop

# Annuler le dernier commit (sans perdre les modifications)
git reset --soft HEAD~1

# ── Maven ─────────────────────────────────────────────────────────────────────

# Compiler et lancer en local (sans Docker)
cd backend/auth-service
mvn spring-boot:run

# Compiler seulement (génère le JAR)
mvn clean package -DskipTests
```

---

*Document maintenu par le Tech Lead. Dernière mise à jour : 2024.*
*Pour toute question, ouvre une issue GitHub.*
