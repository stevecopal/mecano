# 👩‍💻 Guide de Travail — Comment Contribuer au Projet

> Ce document explique **exactement** comment chaque développeur doit travailler
> sur ce projet, de la première fois qu'il clone le repo jusqu'à sa Pull Request.
> Lis ce fichier en entier avant de toucher quoi que ce soit.

---

## Structure du projet (ce que tu vas trouver)

```
mecano/
│
├── backend/
│   ├── auth-service/          ← Authentification & JWT
│   ├── eureka-service/        ← Registre des services (Discovery)
│   ├── gateway-service/       ← Porte d'entrée unique (API Gateway)
│   └── user-service/          ← Profils utilisateurs
│
├── frontend/                  ← Application React
│
├── infrastructure/
│   └── docker/                ← Scripts Docker partagés
│
├── docker-compose.yml         ← Lance tout le système
├── .env.example               ← Template des variables d'environnement
├── .env
├── guide.md
├── .gitignore
└── README.md

```

---

## Qui travaille sur quoi ?

| Développeur | Service | Branche |
|-------------|---------|---------|
| service d'authentification  | `auth-service` | `feature/auth-service` |
|  service gestion d'utilisateur | `user-service` | `feature/user-service` |
|  service eurecka| `eureka-service` | `feature/eureka-service` |
|  service de gateway| `gateway-service` | `feature/gateway-service` |
| Tech Lead | Infrastructure, Docker, Reviews | `develop` |

> **Chaque développeur travaille UNIQUEMENT dans son dossier de service.**
> Tu n'as pas à toucher aux autres dossiers.

---

## Étape 1 — Cloner le dépôt

Une seule fois, quand tu rejoins le projet.

```bash
# Clone le repo sur ta machine
git clone https://github.com/stevecopal/mecano.git

# Entre dans le projet
cd mecano
```

---

## Étape 2 — Se placer sur la bonne branche

> ⚠️ **Règle absolue : tu ne travailles JAMAIS directement sur `develop` ou `main`.**
> Tu travailles uniquement sur ta branche dédiée.

```bash
# Récupère toutes les branches distantes
git fetch origin

# Positionne-toi sur TA branche
# Remplace "auth-service" par ton service
git checkout feature/auth-service

# Vérifie que tu es au bon endroit
git branch
# Tu dois voir :  * feature/auth-service
```

---

## Étape 3 — Copier et configurer ton fichier `.env`

Chaque service a **ses propres variables d'environnement**.
Il y a deux niveaux de `.env` dans ce projet :

### Le `.env` à la racine (global — géré par le Tech Lead)

Il est utilisé uniquement par `docker-compose.yml` pour lancer tout le système.
**Tu n'as pas à le modifier.**

### Le `.env` dans ton service (local — géré par toi)

C'est ton environnement de développement personnel.

```bash
# Entre dans TON dossier de service
cd backend/auth-service

# Copie le template
cp .env.example .env

# Ouvre et remplis le fichier
# (utilise ton éditeur : code .env ou nano .env)
```

**Contenu type d'un `.env` de service :**

```bash
# backend/auth-service/.env

# Base de données locale (pour développer sans Docker)
DB_HOST=localhost
DB_PORT=5432
DB_NAME=auth_db
DB_USERNAME=postgres
DB_PASSWORD=tonMotDePasseLocal

# JWT (clé secrète — mets n'importe quoi en local)
JWT_SECRET=maCleSuperSecreteEnLocalPourDeveloppement

# Port du service
SERVER_PORT=8081

# URL d'Eureka en local
EUREKA_URL=http://localhost:8761/eureka/
```

> **Pourquoi chaque service a son propre `.env` ?**
> Parce que chaque service a sa propre base de données, son propre port,
> ses propres secrets. Un développeur n'a pas besoin des credentials des autres services.
> Et le fichier `.env` n'est jamais commité sur GitHub — il reste sur ta machine.

---

## Étape 4 — Initialiser ton projet Spring Boot dans le bon dossier

> C'est ici que tu crées réellement ton projet. Le dossier existe déjà dans le repo
> (créé par le Tech Lead), mais le code Spring Boot, c'est **toi** qui le génères
> et le places dedans.

```bash
# Tu es dans backend/auth-service/
# Ce dossier est vide (ou contient juste un .gitkeep)
# Tu vas y créer ton projet Spring Boot
```

### Via Spring Initializr (méthode recommandée)

1. Va sur [https://start.spring.io](https://start.spring.io)
2. Configure :
   - **Project** : Maven
   - **Language** : Java
   - **Spring Boot** : 3.2.x
   - **Group** : `com.carplatform`
   - **Artifact** : `auth-service` ← le nom de TON service
   - **Java** : 21
3. Ajoute tes dépendances (selon ton service — voir tableau ci-dessous)
4. Clique **Generate** → extrait le ZIP
5. **Copie le contenu extrait directement dans `backend/auth-service/`**

### Dépendances par service

| Service | Dépendances Spring Initializr |
|---------|-------------------------------|
| `auth-service` | Spring Web, Spring Security, Spring Data JPA, PostgreSQL Driver, Eureka Discovery Client, Validation, Actuator, Lombok |
| `user-service` | Spring Web, Spring Data JPA, PostgreSQL Driver, Eureka Discovery Client, Validation, Actuator, Lombok |
| `eureka-service` | Eureka Server, Actuator |
| `gateway-service` | Gateway, Eureka Discovery Client, Actuator *(⚠️ NE PAS ajouter Spring Web)* |

---

## Étape 5 — Développer dans ton service

Tu travailles **uniquement dans ton dossier** :

```
backend/
└── auth-service/          ← Tout ton travail est ici
    ├── src/
    │   └── main/
    │       ├── java/...   ← Ton code Java
    │       └── resources/
    │           └── application.yml  ← Ta configuration
    ├── pom.xml
    ├── Dockerfile
    └── .env               ← Tes variables locales (jamais commité)
```

### Lancer ton service en local (sans Docker)

```bash
# Assure-toi d'être dans le bon dossier
cd backend/auth-service

# Lance le service
mvn spring-boot:run
```

> **Prérequis pour développer en local :**
> - Java 21 installé (`java -version`)
> - Maven installé (`mvn -version`)
> - PostgreSQL installé et lancé sur ta machine
> - La base de données de ton service créée (`auth_db`, `user_db`...)

### Créer ta base de données locale

```sql
-- Dans PostgreSQL (psql ou pgAdmin) :
CREATE DATABASE auth_db;   -- Pour auth-service
CREATE DATABASE user_db;   -- Pour user-service
```

---

## Étape 6 — Commiter ton travail

> ⚠️ **Tu commites UNIQUEMENT sur ta branche `feature/ton-service`.**
> Jamais sur `develop`, jamais sur `main`.

```bash
# Vérifie toujours sur quelle branche tu es avant de commiter
git branch
# Doit afficher :  * feature/auth-service

# Ajoute tes modifications
git add .

# Crée un commit avec un message clair
git commit -m "feat(auth): ajoute l'endpoint POST /auth/login avec JWT"

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

# Pousse ta branche sur GitHub
git push origin feature/auth-service
```

### Convention des messages de commit

```
type(scope): description courte en présent

Exemples :
feat(auth): ajoute login et génération de token JWT
feat(auth): ajoute register avec validation email
fix(auth): corrige expiration du token JWT
test(auth): ajoute tests unitaires pour JwtService
chore(auth): configure Spring Security
```

| Préfixe | Quand l'utiliser |
|---------|-----------------|
| `feat` | Nouvelle fonctionnalité |
| `fix` | Correction de bug |
| `chore` | Config, setup, dépendances |
| `test` | Ajout de tests |
| `docs` | Documentation |
| `refactor` | Refactoring sans changement fonctionnel |

---

## Étape 7 — Créer une Pull Request vers `main`

Une fois que ta fonctionnalité est prête et testée :

1. Va sur **GitHub → ton repo**
2. Tu verras une bannière jaune : *"feature/auth-service had recent pushes"*
3. Clique **"Compare & pull request"**
4. Configure :
   - **Base** : `main` 
   - **Compare** : `feature/auth-service`
   - **Title** : `feat(auth): implémentation complète du auth-service`
   - **Description** : Explique ce que tu as fait, ce qui reste à faire
5. Clique **"Create pull request"**

Le Tech Lead va :
- Relire ton code
- Tester avec Docker
- Faire des commentaires si nécessaire
- Merger dans `main` quand tout est bon

---

## Étape 8 — Récupérer les mises à jour de `develop`

Quand tes collègues ont mergé leur code dans `develop`,
tu dois mettre à jour ta branche pour éviter les conflits.

```bash
# 1. Récupère les dernières modifications distantes
git fetch origin

# 2. Reste sur ta branche
git checkout feature/auth-service

# 3. Rebase ta branche sur develop
git rebase origin/main

# 4. Si des conflits apparaissent :
#    → Ouvre les fichiers en conflit dans ton éditeur
#    → Résous les conflits manuellement
#    → git add .
#    → git rebase --continue
```

---

## Résumé du flux de travail

```
GitHub (origin)
│
├── main          ← Production (jamais touché directement)
│       ▲
│       │ PR (Tech Lead seulement, après validation)
│
├── develop       ← Intégration de tout le travail
│     
├── feature/auth-service     ← Dev 1 travaille ici
├── feature/user-service     ← Dev 2 travaille ici
├── feature/eureka-service   ← Dev 3 travaille ici
└── feature/gateway-service  ← Dev 4 travaille ici
```

```
Ton flux quotidien :
──────────────────────────────────────────────────────────
1. git checkout feature/MON-SERVICE     ← Toujours vérifier
2. Coder dans backend/mon-service/
3. git add . && git commit -m "feat: ..."
4. git push origin feature/MON-SERVICE
5. (Quand c'est fini) → Pull Request sur GitHub → develop
──────────────────────────────────────────────────────────
```

---

## ❌ Ce qu'il ne faut JAMAIS faire

```bash
# ❌ Ne JAMAIS commiter directement sur develop
git checkout develop
git add .
git commit  # Interdit

# ❌ Ne JAMAIS pousser sur main
git checkout main
git push    # Catastrophe

# ❌ Ne JAMAIS commiter le fichier .env
# Il contient des mots de passe — il reste sur ta machine uniquement

# ❌ Ne JAMAIS modifier le code d'un autre service
# Si tu travailles sur auth-service, ne touche pas user-service
```

---

## ✅ Checklist avant ta première Pull Request

- [ ] Je suis sur la branche `feature/mon-service` (pas sur `develop`)
- [ ] Mon service démarre sans erreur (`mvn spring-boot:run`)
- [ ] Les endpoints de base répondent (test avec Postman ou curl)
- [ ] Le fichier `.env` n'est PAS dans mes commits (`git status` ne le montre pas)
- [ ] Le `Dockerfile` est créé dans mon dossier de service
- [ ] L'application s'enregistre dans Eureka (visible sur `http://localhost:8761`)
- [ ] Mes messages de commit suivent la convention `type(scope): description`
- [ ] Ma PR est bien dirigée vers `develop` (pas `main`)

---

## Questions fréquentes

**Q : Je n'ai pas PostgreSQL installé, comment je développe ?**
> Lance uniquement PostgreSQL via Docker sans lancer tout le système :
> ```bash
> docker compose up postgres
> ```
> Puis lance ton service normalement avec `mvn spring-boot:run`.

**Q : Mon service ne se connecte pas à Eureka en local, que faire ?**
> Vérifie qu'Eureka tourne :
> ```bash
> cd backend/eureka-service && mvn spring-boot:run
> ```
> Puis relance ton service.

**Q : J'ai un conflit lors du rebase, que faire ?**
> Ouvre ton éditeur, cherche les marqueurs `<<<<<<<`, résous le conflit,
> puis : `git add . && git rebase --continue`

**Q : Je dois modifier un fichier en dehors de mon service, que faire ?**
> Crée une issue GitHub ou contacte le Tech Lead.
> Ne modifie jamais le code d'un autre service sans coordination.

---

*Pour toute question, ouvre une **Issue GitHub** dans le repo ou contacte le Tech Lead.*
