# Mecano Backend

## Prérequis

Avant de commencer, assurez-vous d’avoir installé :

* Java 21+
* Docker
* Docker Compose
* Git

Vérifier les installations :

```bash
java -version
docker --version
docker compose version
```

---

# Cloner le projet

```bash
```

---

# Démarrer la base de données

Le projet utilise PostgreSQL avec Docker.

Depuis la racine du projet :

```bash
docker compose up -d
```

Vérifier que le container tourne :

```bash
docker ps
```

Vous devez voir un container PostgreSQL actif.

---

# Lancer les microservices

⚠️ Toujours respecter cet ordre de démarrage.

## 1. Eureka Service

Ouvrir un terminal :

```bash
cd ~/mecano/backend/eureka-service
./mvnw spring-boot:run
```

---

## 2. Auth Service

Ouvrir un nouveau terminal :

```bash
cd ~/mecano/backend/auth-service
./mvnw spring-boot:run
```

---

## 3. User Service

Ouvrir un nouveau terminal :

```bash
cd ~/mecano/backend/user-service
./mvnw spring-boot:run
```

---

## 4. Gateway Service

Ouvrir un nouveau terminal :

```bash
cd ~/mecano/backend/gateway-service
./mvnw spring-boot:run
```

---

# Vérifier Eureka

Ouvrir dans le navigateur :

```txt
http://localhost:8761
```

Les services suivants doivent apparaître :

* AUTH-SERVICE
* USER-SERVICE
* GATEWAY-SERVICE

Status attendu :

```txt
UP
```

---

# Tester les APIs

## User Service

### Directement

```bash
curl http://localhost:8082/api/users/test
```

Réponse attendue :

```txt
user service working
```

### Via Gateway

```bash
curl http://localhost:8080/api/users/test
```

---

## Auth Service

### Directement

```bash
curl http://localhost:8081/api/auth/test
```

### Via Gateway

```bash
curl http://localhost:8080/api/auth/test
```

---

# Arrêter les services

Dans chaque terminal :

```txt
CTRL + C
```

---

# Arrêter PostgreSQL Docker

Depuis la racine du projet :

```bash
docker compose down
```

---

# Rebuild du projet

Si une modification n’est pas détectée :

```bash
./mvnw clean spring-boot:run
```







# Comment utiliser la base de données du projet

Le projet utilise PostgreSQL avec Docker.

Vous **n’avez pas besoin d’installer PostgreSQL localement** ni de créer des bases de données manuellement.

---

## 1. Démarrer la base de données

À la racine du projet :

```bash
docker compose up -d
```

Vérifier :

```bash
docker ps
```

Le container PostgreSQL doit être actif.

---

## 2. Les bases de données sont créées automatiquement

Docker crée automatiquement les databases du projet :

* `auth_db`
* `user_db`

Aucune création manuelle n’est nécessaire.

---

## 3. Lancer les microservices

Chaque service se connecte automatiquement à sa base.

Exemple :

* `auth-service` → `auth_db`
* `user-service` → `user_db`

Lancer les services normalement :

### Eureka

```bash
cd ~/mecano/backend/eureka-service
./mvnw spring-boot:run
```

### Auth Service

```bash
cd ~/mecano/backend/auth-service
./mvnw spring-boot:run
```

### User Service

```bash
cd ~/mecano/backend/user-service
./mvnw spring-boot:run
```

### Gateway

```bash
cd ~/mecano/backend/gateway-service
./mvnw spring-boot:run
```

---

## 4. Voir les données de la base (optionnel)

Entrer dans PostgreSQL :

```bash
docker exec -it mecano-postgres psql -U postgres
```

Lister les bases :

```sql
\l
```

Se connecter à une base :

```sql
\c user_db
```

Afficher les tables :

```sql
\dt
```

Faire une requête SQL :

```sql
SELECT * FROM users;
```

Quitter PostgreSQL :

```sql
\q
```

---

## 5. Réinitialiser complètement la base (si problème)

⚠️ Cette commande supprime toutes les données locales.

```bash
docker compose down -v
docker compose up -d
```
