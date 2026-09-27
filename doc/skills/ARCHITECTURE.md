# Architecture Technique - Micraa v1

## Vue d'Ensemble

Micraa est une plateforme de classes en direct pour établissements scolaires. La première version se concentre sur la communication audio en temps réel de haute qualité.

## État d'Implémentation (Septembre 2026)

- ✅ Auth JWT backend implémentée (`/api/auth/register`, `/api/auth/login`)
- ✅ API sécurisée via Spring Security stateless + filtre JWT
- ✅ Stockage JWT sécurisé côté Flutter
- ✅ Routage post-auth direct vers l'espace du compte connecté
- ✅ Flux classe prof/étudiant fonctionnel (création, start, join, end)
- ✅ Exposition mobile via HTTPS Caddy (`micraa.be` et `livekit.micraa.be`)

## Stack Technique

| Composant | Technologie | Version | Justification |
|-----------|-------------|---------|---------------|
| Backend | Spring Boot | 4.1.1 | Framework Java mature, écosystème riche |
| Language | Java | 21 | LTS, performances, typage fort |
| Database | PostgreSQL | 16 | SGBD relationnel robuste |
| Real-time Audio | LiveKit | latest | Solution WebRTC clé en main, open source |
| Container | Docker | - | Isolation, portabilité, déploiement simplifié |
| Orchestration | Docker Compose | - | Configuration multi-conteneurs simple |

## Architecture Applicative

```
┌─────────────────────────────────────────────────────────┐
│                    Mobile App (Flutter)                 │
│                  Teacher / Student Client                │
└──────────────┬──────────────────────────┬────────────────┘
               │                          │
               │ REST/HTTPS               │ WebRTC (audio)
               │ (Business logic)         │ (Real-time media)
               │                          │
               v                          v
┌──────────────────────────┐   ┌─────────────────────────┐
│   Spring Boot Backend    │   │    LiveKit Server       │
│                          │   │                         │
│  • Authentication        │   │  • WebRTC signaling     │
│  • Authorization         │   │  • Audio routing        │
│  • User Management       │   │  • Participant mgmt     │
│  • Class Management      │   │  • Real-time chat       │
│  • LiveKit Token Gen     │   │  • Connection handling  │
│  • Attendance Tracking   │   │                         │
│  • Webhook Processing    │   │                         │
└──────────┬───────────────┘   └─────────────────────────┘
           │
           │ JDBC
           v
┌──────────────────────────┐
│    PostgreSQL Database   │
│                          │
│  • users                 │
│  • live_classes          │
│  • live_class_students   │
│  • attendance            │
└──────────────────────────┘
```

## Séparation des Responsabilités

### Spring Boot (Backend Business Logic)

**Responsabilités :**
- Gestion des utilisateurs (enseignants, étudiants)
- Gestion des classes en direct (CRUD, statuts)
- Contrôle d'accès et autorisations
- Génération des tokens LiveKit sécurisés
- Enregistrement de la présence
- Traitement des webhooks LiveKit (futur)
- Persistance des données métier

**Ne gère PAS :**
- ❌ Le transport de l'audio/vidéo
- ❌ La signalisation WebRTC
- ❌ La gestion des connexions en temps réel

### LiveKit (Real-time Communication)

**Responsabilités :**
- Signalisation WebRTC
- Routage des flux audio en temps réel
- Gestion des participants connectés
- Gestion des reconnexions
- Chat textuel en temps réel
- Qualité de service (QoS)

**Ne gère PAS :**
- ❌ La logique métier de l'application
- ❌ L'authentification des utilisateurs
- ❌ Les autorisations d'accès aux classes

## Flux d'Authentification et Autorisation

### Flux de Connexion à une Classe

```
┌──────────┐                 ┌──────────────┐              ┌──────────┐
│  Client  │                 │ Spring Boot  │              │ LiveKit  │
└────┬─────┘                 └──────┬───────┘              └────┬─────┘
     │                              │                           │
     │ 1. POST /join (userId)       │                           │
     ├─────────────────────────────>│                           │
     │                              │                           │
     │                              │ 2. Verify user            │
     │                              │ 3. Check authorization    │
     │                              │ 4. Check class status     │
     │                              │                           │
     │                              │ 5. Generate JWT token     │
     │                              │    (with room permissions)│
     │                              │                           │
     │ 6. Return token + room info  │                           │
     │<─────────────────────────────┤                           │
     │                              │                           │
     │ 7. Connect with token        │                           │
     ├──────────────────────────────┼──────────────────────────>│
     │                              │                           │
     │                              │                           │ 8. Validate token
     │                              │                           │ 9. Join room
     │                              │                           │
     │ 10. Audio stream established │                           │
     │<─────────────────────────────┼───────────────────────────┤
     │                              │                           │
```

### Sécurité

1. **API Backend** : Sécurisée avec JWT (routes `/api/auth/**` et `/api/health` publiques)
2. **LiveKit Token** : JWT signé avec secret partagé uniquement côté serveur
3. **Token Lifetime** : 6 heures (configurable)
4. **Room Permissions** : Définies dans le token (canPublish, canSubscribe)

## Modèle de Données

### Schéma Entité-Relation

```
┌─────────────────────┐
│        User         │
├─────────────────────┤
│ id (PK)            │
│ name               │
│ email (UNIQUE)     │
│ passwordHash       │
│ role (ENUM)        │◄─────┐
│ createdAt          │      │
└─────────────────────┘      │
                             │ teacherId (FK)
┌─────────────────────┐      │
│     LiveClass       │      │
├─────────────────────┤      │
│ id (PK)            │      │
│ title              │      │
│ teacherId          │──────┘
│ status (ENUM)      │
│ scheduledAt        │
│ startedAt          │
│ endedAt            │
│ createdAt          │
└─────────┬───────────┘
          │
          │ liveClassId (FK)
          │
┌─────────┴───────────────┐
│  LiveClassStudent       │
├─────────────────────────┤
│ liveClassId (PK, FK)   │
│ studentId (PK, FK)     │
└─────────────────────────┘
          │
          │ studentId (FK)
          │
          └────────────┐
                       │
┌─────────────────────┐│
│     Attendance      ││
├─────────────────────┤│
│ id (PK)            ││
│ liveClassId (FK)   │┘
│ studentId (FK)     │
│ joinedAt           │
│ leftAt             │
└─────────────────────┘
```

### Types Énumérés

**UserRole (cible):**
- `ADMIN`
- `TEACHER`
- `STUDENT`

Un utilisateur peut avoir plusieurs rôles. Le MVP actuel stocke encore un seul rôle et ne contient pas `ADMIN`; la migration du modèle est une priorité de la prochaine implémentation.

**LiveClassStatus:**
- `SCHEDULED` : Classe créée mais pas encore démarrée
- `LIVE` : Classe en cours
- `ENDED` : Classe terminée

## API REST Endpoints

### User Management

| Méthode | Endpoint | Description | Body | Query Params |
|---------|----------|-------------|------|--------------|
| POST | `/api/auth/register` | Inscription + JWT | `{name, email, password, role}` | - |
| POST | `/api/auth/login` | Connexion + JWT | `{email, password}` | - |
| POST | `/api/users` | Créer utilisateur | `{name, email, role}` | - |
| GET | `/api/users` | Liste tous utilisateurs | - | - |
| GET | `/api/users/teachers` | Liste enseignants | - | - |
| GET | `/api/users/students` | Liste étudiants | - | - |
| GET | `/api/users/{id}` | Détails utilisateur | - | - |

### Live Class Management

| Méthode | Endpoint | Description | Body | Query Params |
|---------|----------|-------------|------|--------------|
| POST | `/api/live-classes` | Créer classe | `{title, teacherId, scheduledAt}` | - |
| GET | `/api/live-classes` | Liste classes | - | `userId, role` |
| GET | `/api/live-classes/{id}` | Détails classe | - | - |
| POST | `/api/live-classes/{id}/students` | Ajouter étudiants | `{studentIds[]}` | - |
| POST | `/api/live-classes/{id}/start` | Démarrer classe | - | `teacherId` |
| POST | `/api/live-classes/{id}/join` | Rejoindre (get token) | - | `userId` |
| POST | `/api/live-classes/{id}/end` | Terminer classe | - | `teacherId` |

### Health & Monitoring

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/api/health` | Health check |

## Configuration

### Application Properties

```properties
# Database
spring.datasource.url=jdbc:postgresql://postgres:5432/micraa
spring.datasource.username=micraa
spring.datasource.password=micraa123

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update  # Crée/met à jour les tables auto

# LiveKit
livekit.url=ws://livekit:7880
livekit.api.key=devkey
livekit.api.secret=secret  # NE JAMAIS exposer au client !
```

### Docker Compose Services

| Service | Image | Ports | Description |
|---------|-------|-------|-------------|
| postgres | postgres:15 | 5433 (hôte) → 5432 (conteneur) | Base de données |
| livekit | livekit/livekit-server:latest | 7880, 7881, 50000-50100/udp | Serveur WebRTC |
| app | Custom (Dockerfile) | 8080 | Application Spring Boot |

### Exposition HTTPS de production

- API REST : `https://micraa.be/api`
- LiveKit : `wss://livekit.micraa.be`
- Caddy termine TLS et reverse-proxy l'API vers `app:8080` et LiveKit vers `livekit:7880`.
- Le client mobile ne reçoit jamais la clé ou le secret API LiveKit.

## Décisions Techniques Clés

### 1. Pourquoi LiveKit ?

✅ **Avantages :**
- Solution WebRTC complète et prête à l'emploi
- Open source avec support commercial disponible
- SDK pour de nombreuses plateformes (Flutter, iOS, Android, Web)
- Gestion automatique de la qualité réseau
- Support du chat textuel intégré
- Documentation excellente

❌ **Alternatives écartées :**
- Twilio Video : Coûteux, vendor lock-in
- Jitsi : Plus complexe à déployer et personnaliser
- Agora : Pricing model moins transparent

### 2. Pourquoi Spring Boot 4.x ?

✅ **Avantages :**
- Framework mature et éprouvé
- Écosystème très riche
- Support natif de GraalVM (performances)
- Excellente intégration avec PostgreSQL
- Communauté large

### 3. Pourquoi PostgreSQL ?

✅ **Avantages :**
- SGBD relationnel robuste
- Excellent support des contraintes
- Performance pour notre volumétrie
- Extensions disponibles si besoin (PostGIS, etc.)

### 4. Architecture Monolithique Modulaire

Pour la v1, nous adoptons une architecture monolithique modulaire :

✅ **Justification :**
- Simplicité de déploiement
- Moins de complexité opérationnelle
- Transactions ACID faciles
- Performance (pas de latence réseau inter-services)
- Évolution possible vers microservices si nécessaire

**Structure modulaire :**
```
com.wiseintech.micraa/
├── model/          # Entités JPA
├── repository/     # Accès données
├── service/        # Logique métier
├── controller/     # API REST
├── dto/            # Objets de transfert
├── config/         # Configuration
└── exception/      # Gestion erreurs
```

## Patterns et Bonnes Pratiques

### 1. Repository Pattern
- Isolation de la couche d'accès aux données
- Utilisation de Spring Data JPA

### 2. Service Layer
- Logique métier isolée des contrôleurs
- Transactions gérées au niveau service

### 3. DTO Pattern
- Séparation entre modèle de données et API
- Évite l'exposition directe des entités JPA

### 4. Exception Handling
- Gestionnaire global avec `@RestControllerAdvice`
- Messages d'erreur cohérents

### 5. Logging
- Utilisation de SLF4J + Logback
- Logs structurés avec contexte

## Scalabilité Future

### Axes d'amélioration identifiés :

1. **Caching** : Redis pour sessions et données fréquentes
2. **Queue System** : RabbitMQ/Kafka pour webhooks LiveKit
3. **CDN** : Pour assets statiques
4. **Load Balancer** : Nginx pour distribution charge
5. **Metrics** : Prometheus + Grafana
6. **Tracing** : OpenTelemetry

### Migration Microservices (si nécessaire) :

```
Monolith → Microservices potentiels
├── User Service (Auth, Users)
├── Class Service (LiveClasses, Attendance)
├── LiveKit Service (Token generation, Webhooks)
├── Notification Service (Email, Push)
└── Analytics Service (Reporting, Statistics)
```

## Limitations Connues (v1)

1. ⚠️ Pas de refresh token / rotation (JWT access token simple)
2. ⚠️ Pas de gestion des webhooks LiveKit
3. ⚠️ Pas de vidéo
4. ⚠️ Pas d'enregistrement
5. ⚠️ Pas de partage d'écran
6. ⚠️ Pas d'analytics avancés
7. ⚠️ Pas de notifications push
8. ⚠️ Pas de gestion des permissions granulaires par matière/groupe
9. ⚠️ Inscription étudiant MVP: auto-inscription des étudiants existants à la création d'une classe (pas encore de roster UI par classe)
10. ⚠️ Intégration Moodle non incluse en v1; prévue comme intégration séparée en v2

Ces limitations seront adressées dans les versions futures selon les priorités métier.

## Préparation de l'intégration Moodle v2

Moodle sera intégré en v2, sans devenir une dépendance du cœur de Micraa v1. L'architecture doit donc isoler cette future intégration dans un module ou package dédié, avec des adaptateurs pour les API Moodle et la synchronisation.

Principes à préserver dès maintenant :

- les entités Micraa conservent leurs propres identifiants et leur propre cycle de vie ;
- les références externes Moodle sont ajoutées séparément et ne remplacent pas les clés primaires ;
- la synchronisation doit être idempotente, rejouable et journalisée ;
- les secrets, tokens et comptes de service Moodle restent côté backend ;
- Flutter ne communique jamais directement avec Moodle pour les opérations sensibles ;
- Micraa reste responsable des classes live, des autorisations de participation et des tokens LiveKit.

## Prochaines Étapes Techniques

### Phase 1 (Actuelle) : MVP Audio
- ✅ Architecture de base
- ✅ API REST
- ✅ Intégration LiveKit
- ⏳ Tests audio avec client Flutter

### Phase 2 : Authentification & Sécurité
- JWT Authentication
- Role-based Access Control
- Password hashing (BCrypt)
- API Security (HTTPS, CORS)

### Phase 3 : Webhooks & Monitoring
- LiveKit webhooks
- Health checks avancés
- Métriques (Micrometer/Prometheus)
- Logging structuré

### Phase 4 : Features Avancées
- Chat textuel persistant
- Enregistrement des sessions
- Vidéo support
- Screen sharing

## Ressources Techniques

### Documentation
- Spring Boot: https://spring.io/projects/spring-boot
- LiveKit: https://docs.livekit.io/
- PostgreSQL: https://www.postgresql.org/docs/

### Repositories
- LiveKit Server: https://github.com/livekit/livekit
- LiveKit Server SDK Java: https://github.com/livekit/server-sdk-java

### Monitoring
- Application logs: `docker-compose logs -f app`
- Database logs: `docker-compose logs -f postgres`
- LiveKit logs: `docker-compose logs -f livekit`

## Contact & Support

Pour questions techniques :
- Consulter le [guide Docker](../operations/DOCKER.md)
- Vérifier les logs : `make logs`
- Tester l'API : `make test`
