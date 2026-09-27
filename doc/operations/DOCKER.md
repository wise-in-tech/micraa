# Micraa - School Live Classroom Platform

## Description

Plateforme d'e-learning pour établissements scolaires avec classes en direct utilisant LiveKit pour l'audio en temps réel.

## Architecture

- **Backend**: Spring Boot (Java 21)
- **Database**: PostgreSQL 15
- **Real-time Audio**: LiveKit
- **Deployment**: Docker Compose

## État Actuel (Septembre 2026)

- ✅ Authentification JWT active (`/api/auth/register`, `/api/auth/login`)
- ✅ API sécurisée via Spring Security + filtre JWT
- ✅ Stockage sécurisé du token côté Flutter (`flutter_secure_storage`)
- ✅ Redirection post-login vers l'espace utilisateur connecté (plus de sélection libre de profil)
- ✅ Création de classe enseignant + visibilité étudiant (auto-inscription MVP)
- ✅ API publique HTTPS : `https://micraa.be`
- ✅ LiveKit public sécurisé : `wss://livekit.micraa.be`

## Règles métier et rôles

Micraa prévoit trois rôles :

- **ADMIN** : gère la plateforme, crée les comptes enseignants, crée et planifie les classes.
- **TEACHER** : anime les classes qui lui sont attribuées et peut les démarrer et les terminer.
- **STUDENT** : crée son compte en ligne, consulte les classes auxquelles il a accès et les rejoint.

Un utilisateur peut posséder un ou plusieurs rôles. Les permissions doivent être cumulatives et contrôlées côté backend, pas uniquement dans l'interface mobile.

Règles de création de comptes :

- l'inscription publique depuis l'application crée uniquement un compte **STUDENT** ;
- seul un **ADMIN** peut créer un compte **TEACHER** ;
- la création d'un compte ADMIN est réservée à l'administration de la plateforme.

Règles de classes :

- seul un **ADMIN** peut créer et planifier une classe ;
- un **TEACHER** peut gérer et animer les classes qui lui sont attribuées ;
- un **STUDENT** ne peut ni créer ni planifier une classe.

### Mot de passe et e-mail

L'utilisateur peut modifier son mot de passe depuis l'application mobile. Il peut également demander un lien de réinitialisation par e-mail. Pour la première version, les e-mails Micraa seront envoyés depuis une adresse Google dédiée de type `no-reply`, à configurer dans l'environnement de déploiement. Les identifiants SMTP ne doivent jamais être stockés dans le dépôt.

> **État d'implémentation (ticket-001) :** `ADMIN` existe désormais comme rôle, mais un utilisateur ne stocke encore qu'un seul rôle. L'inscription publique (`POST /api/auth/register`) ignore tout `role` transmis par le client et crée toujours un `STUDENT`. La création d'un compte `TEACHER` est réservée à `POST /api/admin/teachers`, protégé par `@PreAuthorize("hasRole('ADMIN')")`. Il n'existe aucun endpoint capable de créer un `ADMIN` : le tout premier compte ADMIN est inséré directement en base par un script SQL (haché en bcrypt, hors du code applicatif), pas par l'application elle-même. Restent à implémenter : les rôles multiples par utilisateur, la restriction de la création/planification de classe à `ADMIN`, et le changement/la réinitialisation de mot de passe.

## Prochaine implémentation backend

1. Ajouter une relation permettant plusieurs rôles par utilisateur (actuellement un seul rôle, `ADMIN` inclus dans l'énumération).
2. ~~Forcer l'inscription publique à créer uniquement un compte `STUDENT`.~~ ✅ Fait (ticket-001).
3. ~~Réserver la création des comptes `TEACHER` à `ADMIN`.~~ ✅ Fait pour `TEACHER` (ticket-001) — réserver aussi la création/planification des classes à `ADMIN` reste à faire.
4. Permettre à `TEACHER` de gérer, démarrer et terminer les classes qui lui sont assignées (déjà en place pour start/end ; l'ownership control complet reste à affiner).
5. Ajouter le changement de mot de passe authentifié et la réinitialisation par lien e-mail à usage unique et durée limitée.
6. Configurer une adresse Google dédiée `no-reply` via variables d'environnement; ne jamais versionner les identifiants.
7. ~~Ajouter des tests d'autorisation pour chaque rôle.~~ ✅ Fait pour l'inscription et la création de `TEACHER` (ticket-001) ; à compléter pour classes/mot de passe au fil des prochaines implémentations.

### Contrat API cible

| Méthode | Endpoint | Accès | Fonction |
|---------|----------|-------|----------|
| POST | `/api/auth/register` | Public | Créer uniquement un `STUDENT` |
| POST | `/api/auth/login` | Public | Authentifier un utilisateur |
| POST | `/api/auth/change-password` | Authentifié | Modifier son mot de passe |
| POST | `/api/auth/forgot-password` | Public | Envoyer un lien de réinitialisation |
| POST | `/api/auth/reset-password` | Public avec token | Définir un nouveau mot de passe |
| POST | `/api/admin/teachers` | `ADMIN` | Créer un compte enseignant ✅ implémenté |
| POST | `/api/live-classes` | `ADMIN` (cible) | Créer et planifier une classe — actuellement ouvert à tout utilisateur authentifié |
| POST | `/api/live-classes/{id}/start` | Enseignant assigné | Démarrer sa classe |
| POST | `/api/live-classes/{id}/end` | Enseignant assigné | Terminer sa classe |
| GET | `/api/live-classes/{id}/participants` | Enseignant assigné | Lister les participants connectés, triés par heure de connexion ✅ implémenté |
| POST | `/api/live-classes/{id}/participants/{participantId}/mute` | Enseignant assigné | Couper le micro d'un étudiant ✅ implémenté |
| POST | `/api/live-classes/{id}/participants/{participantId}/unmute` | Enseignant assigné | Réactiver le micro d'un étudiant ✅ implémenté |

Les tokens de réinitialisation doivent être stockés sous forme hachée, expirer rapidement et être invalidés après utilisation.

### Provisionnement du premier compte ADMIN

Aucun endpoint ne peut créer un `ADMIN` (un rôle transmis dans une requête n'est jamais une preuve d'autorisation, et il n'existe encore aucun ADMIN pour en autoriser un premier). Le tout premier compte ADMIN est créé par un script SQL exécuté directement sur la base `users`, avec :

- `password_hash` = un hash BCrypt (le même algorithme que `SecurityConfig.passwordEncoder()`, coût 12) ;
- `role` = `ADMIN` ;
- `created_at` renseigné (colonne `NOT NULL`).

Une fois ce compte inséré, il se connecte normalement via `POST /api/auth/login` et utilise le JWT obtenu (rôle `ADMIN`) pour appeler `POST /api/admin/teachers`.

## Prérequis

- Docker Desktop
- Docker Compose
- Java 21 (pour développement local)
- Maven 3.9+ (pour développement local)

## Démarrage avec Docker

### 1. Construire et démarrer tous les services

```bash
docker-compose up --build
```

Cette commande va :
- Démarrer PostgreSQL sur le port 5433 (hôte) → 5432 (conteneur)
- Démarrer LiveKit sur le port 7880
- Construire et démarrer l'application Spring Boot sur le port 8080

### 2. Vérifier que les services sont démarrés

```bash
# Vérifier les containers
docker ps

# Vérifier la santé de l'application
curl http://localhost:8080/api/health
```

### 3. Arrêter les services

```bash
docker-compose down
```

### 4. Arrêter et supprimer les volumes (réinitialisation complète)

```bash
docker-compose down -v
```

## Tests de l'API

### 0. (Admin) Créer un compte enseignant

Un compte `ADMIN` a été inséré au préalable par script SQL (voir [Provisionnement du premier compte ADMIN](#provisionnement-du-premier-compte-admin)). On se connecte avec, puis on crée un enseignant :

```bash
# Connexion en tant qu'admin
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@micraa.local", "password": "<mot-de-passe-admin>"}'

# Avec le token obtenu :
curl -X POST http://localhost:8080/api/admin/teachers \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <admin-jwt>" \
  -d '{
    "name": "Prof. Martin",
    "email": "teacher@school.fr",
    "password": "SecurePass123!"
  }'
```

> Sans JWT valide : `401`. Avec un JWT `STUDENT` ou `TEACHER` : `403`.

### 1. Inscription publique (crée toujours un STUDENT)

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Alice",
    "email": "alice@school.fr",
    "password": "SecurePass123!"
  }'
```

> Envoyer `"role": "TEACHER"` ou `"role": "ADMIN"` dans ce corps n'a aucun effet : le compte créé est toujours `STUDENT`.

### 2. Se connecter et récupérer le token

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "teacher@school.fr",
    "password": "SecurePass123!"
  }'
```

> Copier le champ `token` de la réponse et l'utiliser dans l'en-tête `Authorization: Bearer <token>`.

### 3. Créer une classe en direct

```bash
curl -X POST http://localhost:8080/api/live-classes \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <teacher-jwt>" \
  -d '{
    "title": "Mathématiques - Algèbre",
    "teacherId": 1,
    "scheduledAt": "2026-08-21T10:00:00"
  }'
```

### 4. Démarrer la classe (enseignant)

```bash
curl -X POST "http://localhost:8080/api/live-classes/1/start?teacherId=1" \
  -H "Authorization: Bearer <teacher-jwt>"
```

### 5. Rejoindre la classe (enseignant ou étudiant)

```bash
curl -X POST "http://localhost:8080/api/live-classes/1/join?userId=1" \
  -H "Authorization: Bearer <teacher-jwt>"
```

### 6. Lister les classes

```bash
# Classes de l'enseignant
curl "http://localhost:8080/api/live-classes?userId=1&role=TEACHER" \
  -H "Authorization: Bearer <teacher-jwt>"
```

### 7. Lister les participants connectés (enseignant assigné uniquement)

```bash
curl "http://localhost:8080/api/live-classes/1/participants" \
  -H "Authorization: Bearer <teacher-jwt>"
```

> Réponse triée par heure de connexion (la plus ancienne en premier), sans jeton ni secret. `403` si l'appelant n'est pas l'enseignant assigné à la classe.

### 8. Couper / réactiver le microphone d'un étudiant (enseignant assigné uniquement)

```bash
# Couper le micro de l'étudiant 10
curl -X POST "http://localhost:8080/api/live-classes/1/participants/10/mute" \
  -H "Authorization: Bearer <teacher-jwt>"

# Le réactiver
curl -X POST "http://localhost:8080/api/live-classes/1/participants/10/unmute" \
  -H "Authorization: Bearer <teacher-jwt>"
```

> Appliqué côté serveur via l'API d'administration LiveKit (`RoomServiceClient.mutePublishedTrack`) : l'étudiant ne peut pas réactiver lui-même un micro coupé par l'enseignant. `403` si l'appelant n'est pas l'enseignant assigné (y compris pour un étudiant qui tenterait de couper un autre participant).

### 9. Terminer la classe

```bash
curl -X POST "http://localhost:8080/api/live-classes/1/end?teacherId=1" \
  -H "Authorization: Bearer <teacher-jwt>"
```

## Structure du Projet

```
micraa/
├── src/main/java/com/wiseintech/micraa/
│   ├── config/          # Configuration (LiveKit, etc.)
│   ├── controller/      # REST Controllers
│   ├── dto/             # Data Transfer Objects
│   ├── exception/       # Exception Handlers
│   ├── model/           # JPA Entities
│   ├── repository/      # Spring Data Repositories
│   └── service/         # Business Logic
├── src/main/resources/
│   └── application.properties
├── Dockerfile
├── docker-compose.yml
└── pom.xml
```

## Modèle de Données

### User
- id, name, email, passwordHash, role, createdAt
- MVP actuel : rôle unique `TEACHER` ou `STUDENT`
- Cible : un ou plusieurs rôles parmi `ADMIN`, `TEACHER`, `STUDENT`

### LiveClass
- id, title, teacherId, status, scheduledAt, startedAt, endedAt, createdAt
- Statuts: SCHEDULED, LIVE, ENDED

### LiveClassStudent
- liveClassId, studentId (composite key)

### Attendance
- id, liveClassId, studentId, joinedAt, leftAt

## API Endpoints

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/api/health` | Vérifier la santé de l'application |
| POST | `/api/auth/register` | Inscription utilisateur (toujours `STUDENT`) + JWT |
| POST | `/api/auth/login` | Connexion utilisateur + JWT |
| POST | `/api/admin/teachers` | Créer un compte enseignant (réservé à `ADMIN`) |
| GET | `/api/users` | Lister tous les utilisateurs |
| GET | `/api/users/teachers` | Lister les enseignants |
| GET | `/api/users/students` | Lister les étudiants |
| POST | `/api/live-classes` | Créer une classe |
| GET | `/api/live-classes` | Lister les classes (par userId et role) |
| GET | `/api/live-classes/{id}` | Obtenir les détails d'une classe |
| POST | `/api/live-classes/{id}/students` | Ajouter des étudiants |
| POST | `/api/live-classes/{id}/start` | Démarrer une classe (enseignant) |
| POST | `/api/live-classes/{id}/join` | Rejoindre une classe (obtenir token LiveKit) |
| POST | `/api/live-classes/{id}/end` | Terminer une classe (enseignant) |
| GET | `/api/live-classes/{id}/participants` | Lister les participants connectés, triés par heure de connexion (enseignant assigné uniquement) |
| POST | `/api/live-classes/{id}/participants/{participantId}/mute` | Couper le micro d'un étudiant (enseignant assigné uniquement) |
| POST | `/api/live-classes/{id}/participants/{participantId}/unmute` | Réactiver le micro d'un étudiant (enseignant assigné uniquement) |

> La légacy `POST /api/users` (qui permettait de créer un utilisateur avec un rôle arbitraire, y compris `TEACHER`) a été retirée. `POST /api/users` renvoie désormais `405 Method Not Allowed`.

## Configuration LiveKit

L'application utilise LiveKit pour la communication audio en temps réel. La configuration par défaut utilise :

- **URL**: ws://livekit:7880 (dans Docker) ou ws://localhost:7880 (en local)
- **API Key**: devkey
- **API Secret**: secret

Ces valeurs sont définies dans `docker-compose.yml` et `application.properties`.

## Développement Local (sans Docker)

### 1. Démarrer PostgreSQL localement

```bash
# Avec Docker
docker run --name micraa-postgres \
  -e POSTGRES_DB=micraa \
  -e POSTGRES_USER=micraa \
  -e POSTGRES_PASSWORD=micraa123 \
  -p 5432:5432 \
  -d postgres:16-alpine
```

### 2. Démarrer LiveKit localement

```bash
docker run --name micraa-livekit \
  -p 7880:7880 \
  -p 7881:7881 \
  -p 50000-50100:50000-50100/udp \
  -e LIVEKIT_KEYS="devkey: secret" \
  livekit/livekit-server:latest \
  --dev --bind 0.0.0.0
```

### 3. Démarrer l'application Spring Boot

```bash
./mvnw spring-boot:run
```

### 4. Lancer les tests backend

```bash
./mvnw test
```

Les tests tournent contre une base H2 en mémoire (`src/test/resources/application.properties`) : ni PostgreSQL ni LiveKit ne doivent être démarrés. Ils couvrent notamment (ticket-001) : inscription publique → toujours `STUDENT`, `POST /api/admin/teachers` → `401`/`403`/`200` selon l'appelant, suppression de la légacy `POST /api/users`, tri des participants par heure de connexion, et les règles d'autorisation du mute/unmute du microphone.

## Logs et Débogage

### Voir les logs de l'application

```bash
docker-compose logs -f app
```

### Voir les logs de PostgreSQL

```bash
docker-compose logs -f postgres
```

### Voir les logs de LiveKit

```bash
docker-compose logs -f livekit
```

## Accès mobile en production

L'application mobile utilise les endpoints publics permanents :

- API REST : `https://micraa.be/api`
- LiveKit : `wss://livekit.micraa.be`

Vérification rapide :

```bash
curl https://micraa.be/api/health
```

## Prochaines Étapes

- [ ] Test audio avec une vraie application mobile Flutter
- [ ] Implémenter les webhooks LiveKit
- [ ] Ajouter la gestion du chat texte
- [x] Ajouter la liste des participants en temps réel (ticket-001 : `GET /api/live-classes/{id}/participants`, côté enseignant)
- [x] Tests unitaires et d'intégration (ticket-001 : rôles/inscription/participants/micro ; à étendre aux prochaines fonctionnalités)
- [ ] Implémenter les rôles multiples par utilisateur (le rôle `ADMIN` existe désormais dans l'énumération, mais un seul rôle par utilisateur)
- [ ] Implémenter les permissions de création et planification des classes (réservé à `ADMIN`)
- [ ] Implémenter le changement et la réinitialisation du mot de passe
- [ ] Configurer l'adresse Google no-reply

## Notes Importantes

- **Sécurité**: L'API est sécurisée par JWT. Les routes `/api/auth/**` et `/api/health` sont publiques; le reste nécessite `Authorization: Bearer <token>`. Un appel sans JWT valide reçoit `401`; un appel authentifié mais sans le rôle/l'autorisation requis reçoit `403`.
- **LiveKit**: Le secret API ne doit JAMAIS être exposé au client mobile. Les tokens sont générés côté serveur. La liste des participants et le contrôle du microphone passent par l'API d'administration LiveKit (`RoomServiceClient`), toujours côté serveur.
- **Audio**: L'audio transite directement entre LiveKit et les clients (pas par Spring Boot).

### Limitation MVP actuelle

- Lorsqu'une classe est créée, les étudiants existants sont auto-inscrits (pas encore de roster par classe dans l'UI Flutter).
- `POST /api/live-classes`, `/start`, `/end` et `/join` font encore confiance à un `teacherId`/`userId` transmis en paramètre plutôt qu'à l'identité issue du JWT (contrairement aux nouveaux endpoints participants/mute/unmute, qui utilisent l'identité authentifiée). À corriger dans une prochaine itération.

## Support

Pour tout problème, vérifier :
1. Les logs Docker : `docker-compose logs`
2. La santé de l'API : `curl http://localhost:8080/api/health`
3. Que PostgreSQL est accessible : `docker exec -it micraa-postgres psql -U micraa -d micraa`
