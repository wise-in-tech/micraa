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
- ✅ Accès mobile via Cloudflare Tunnels (App/API/LiveKit)

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

### 1. Créer un compte enseignant (JWT)

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Prof. Martin",
    "email": "teacher@school.fr",
    "password": "SecurePass123!",
    "role": "TEACHER"
  }'
```

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

### 5. Rejoindre la classe (enseignant)

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

### 8. Terminer la classe

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
- Rôles: TEACHER, STUDENT

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
| POST | `/api/auth/register` | Inscription utilisateur + JWT |
| POST | `/api/auth/login` | Connexion utilisateur + JWT |
| POST | `/api/users` | Créer un utilisateur (legacy / test) |
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

## Accès depuis téléphone avec Cloudflare Tunnel

Quand le téléphone ne peut pas accéder directement aux ports locaux (`4000`, `8080`, `7880`), utiliser 3 tunnels Cloudflare.

### 1) Lancer le serveur web Flutter

```bash
cd /Users/mohy/sofiane-devenv/micraa_flutter/build/web
python3 -m http.server 4000
```

### 2) Démarrer les 3 tunnels

Dans 3 terminaux séparés :

```bash
cloudflared tunnel --url http://localhost:4000
```

```bash
cloudflared tunnel --url http://localhost:8080
```

```bash
cloudflared tunnel --url http://localhost:7880
```

Récupérer les URLs `https://...trycloudflare.com` affichées dans chaque terminal.

### 3) Mettre à jour la config Flutter

Dans `micraa_flutter/lib/config/app_config.dart` :

```dart
const String? kCloudflaredApiUrl = 'https://<api-tunnel>.trycloudflare.com';
const String? kCloudflaredLiveKitUrl = 'wss://<livekit-tunnel>.trycloudflare.com';
```

Puis reconstruire Flutter web :

```bash
cd /Users/mohy/sofiane-devenv/micraa_flutter
flutter clean
flutter pub get
flutter build web --release
```

### 4) Vérifier que tout est accessible

```bash
pgrep -af cloudflared
```

```bash
curl -I https://<app-tunnel>.trycloudflare.com
```

```bash
curl -I https://<api-tunnel>.trycloudflare.com/api/health
```

```bash
curl -I https://<livekit-tunnel>.trycloudflare.com/rtc/validate
```

> Note: un `401` sur `/rtc/validate` est normal sans token LiveKit et confirme que le tunnel LiveKit répond.

### 5) Problèmes fréquents

- **`ERR_NAME_NOT_RESOLVED`** : tunnel expiré → relancer `cloudflared` et mettre à jour les URLs.
- **`Failed to fetch` en join session** : URL LiveKit expirée/invalide ou non recompilée côté Flutter.
- **Listing de fichiers au lieu de l'app** : mauvais répertoire servi par `http.server` (doit être `build/web`).

## Prochaines Étapes

- [ ] Test audio avec une vraie application mobile Flutter
- [ ] Implémenter les webhooks LiveKit
- [ ] Ajouter la gestion du chat texte
- [ ] Ajouter la liste des participants en temps réel
- [ ] Tests unitaires et d'intégration
- [ ] Remplacer les tunnels quick (`trycloudflare`) par un tunnel nommé stable

## Notes Importantes

- **Sécurité**: L'API est sécurisée par JWT. Les routes `/api/auth/**` et `/api/health` sont publiques; le reste nécessite `Authorization: Bearer <token>`.
- **LiveKit**: Le secret API ne doit JAMAIS être exposé au client mobile. Les tokens sont générés côté serveur.
- **Audio**: L'audio transite directement entre LiveKit et les clients (pas par Spring Boot).

### Limitation MVP actuelle

- Lorsqu'une classe est créée, les étudiants existants sont auto-inscrits (pas encore de roster par classe dans l'UI Flutter).

## Support

Pour tout problème, vérifier :
1. Les logs Docker : `docker-compose logs`
2. La santé de l'API : `curl http://localhost:8080/api/health`
3. Que PostgreSQL est accessible : `docker exec -it micraa-postgres psql -U micraa -d micraa`
