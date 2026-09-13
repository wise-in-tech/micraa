# Diagrammes d'Architecture - Micraa

## Vue d'Ensemble du Système

```
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃                    MOBILE APPLICATIONS                      ┃
┃  ┌─────────────────────┐      ┌─────────────────────┐     ┃
┃  │   Teacher App       │      │    Student App      │     ┃
┃  │   (Flutter)         │      │    (Flutter)        │     ┃
┃  └──────────┬──────────┘      └──────────┬──────────┘     ┃
┗━━━━━━━━━━━━━┿━━━━━━━━━━━━━━━━━━━━━━━━━━━┿━━━━━━━━━━━━━━━━┛
               │                            │
               │ REST/HTTPS                 │ REST/HTTPS
               │ (Business Logic)           │ (Business Logic)
               ▼                            ▼
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃                   SPRING BOOT BACKEND                       ┃
┃                    (localhost:8080)                         ┃
┃  ┌────────────────────────────────────────────────────┐    ┃
┃  │                 REST API Layer                      │    ┃
┃  │  • UserController                                   │    ┃
┃  │  • LiveClassController                              │    ┃
┃  │  • HealthController                                 │    ┃
┃  └─────────────────┬──────────────────────────────────┘    ┃
┃                    │                                        ┃
┃  ┌─────────────────▼──────────────────────────────────┐    ┃
┃  │              Service Layer                          │    ┃
┃  │  • UserService                                      │    ┃
┃  │  • LiveClassService                                 │    ┃
┃  │  • LiveKitService (Token Generation)                │    ┃
┃  └─────────────────┬──────────────────────────────────┘    ┃
┃                    │                                        ┃
┃  ┌─────────────────▼──────────────────────────────────┐    ┃
┃  │           Repository Layer                          │    ┃
┃  │  • UserRepository                                   │    ┃
┃  │  • LiveClassRepository                              │    ┃
┃  │  • LiveClassStudentRepository                       │    ┃
┃  │  • AttendanceRepository                             │    ┃
┃  └─────────────────┬──────────────────────────────────┘    ┃
┗━━━━━━━━━━━━━━━━━━━━┿━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
                     │ JDBC
                     ▼
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃                  POSTGRESQL DATABASE                        ┃
┃                    (localhost:5432)                         ┃
┃  ┌─────────────┐ ┌──────────────┐ ┌──────────────────┐    ┃
┃  │   users     │ │ live_classes │ │ live_class_      │    ┃
┃  │             │ │              │ │   students       │    ┃
┃  └─────────────┘ └──────────────┘ └──────────────────┘    ┃
┃                  ┌──────────────┐                          ┃
┃                  │  attendance  │                          ┃
┃                  └──────────────┘                          ┃
┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛


┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃              LIVEKIT SERVER (WebRTC)                        ┃
┃                (localhost:7880)                             ┃
┃  ┌────────────────────────────────────────────────────┐    ┃
┃  │           Real-Time Media Routing                  │    ┃
┃  │  • Audio Streaming                                 │    ┃
┃  │  • WebRTC Signaling                                │    ┃
┃  │  • Participant Management                          │    ┃
┃  │  • Chat (Real-time)                                │    ┃
┃  └────────────────────────────────────────────────────┘    ┃
┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
          ▲                                    ▲
          │ WebSocket + WebRTC                 │
          │ (Audio Streams)                    │
┌─────────┴───────────┐            ┌───────────┴──────────┐
│   Teacher Device    │            │   Student Device     │
│   Microphone ◄─►    │            │   Microphone ◄─►     │
│   Speaker           │            │   Speaker            │
└─────────────────────┘            └──────────────────────┘
```

## Flux de Données : Rejoindre une Classe

```
┌──────────┐                 ┌──────────────┐                ┌──────────┐
│  Mobile  │                 │ Spring Boot  │                │ LiveKit  │
│   App    │                 │   Backend    │                │  Server  │
└────┬─────┘                 └──────┬───────┘                └────┬─────┘
     │                              │                             │
     │ 1. GET /live-classes?        │                             │
     │    userId=2&role=STUDENT     │                             │
     ├─────────────────────────────>│                             │
     │                              │ Query DB                    │
     │ 2. List of classes           │                             │
     │<─────────────────────────────┤                             │
     │                              │                             │
     │ 3. POST /live-classes/       │                             │
     │    1/join?userId=2           │                             │
     ├─────────────────────────────>│                             │
     │                              │                             │
     │                              │ 4. Verify:                  │
     │                              │   - User exists             │
     │                              │   - Class is LIVE           │
     │                              │   - User authorized         │
     │                              │                             │
     │                              │ 5. Generate LiveKit Token:  │
     │                              │   - Room: "class-1"         │
     │                              │   - Identity: "student-2"   │
     │                              │   - Permissions: canPublish │
     │                              │   - TTL: 6 hours            │
     │                              │                             │
     │                              │ 6. Record attendance        │
     │                              │                             │
     │ 7. Return:                   │                             │
     │   {livekitUrl, token, room}  │                             │
     │<─────────────────────────────┤                             │
     │                              │                             │
     │ 8. Connect to LiveKit        │                             │
     ├──────────────────────────────┼────────────────────────────>│
     │   ws://localhost:7880        │                             │
     │   + JWT token                │                             │
     │                              │                             │ 9. Validate JWT
     │                              │                             │    (signature,
     │                              │                             │     expiry,
     │                              │                             │     permissions)
     │                              │                             │
     │ 10. Audio channel established│                             │
     │<─────────────────────────────┼─────────────────────────────┤
     │                              │                             │
     │ 11. Real-time audio flow     │                             │
     │<═════════════════════════════╬═════════════════════════════>│
     │    (WebRTC - bypasses API)   │                             │
     │                              │                             │
```

## Flux Audio en Temps Réel

```
                    LIVEKIT SERVER
                         │
                         │
        ┌────────────────┼────────────────┐
        │                │                │
        ▼                ▼                ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│   Teacher    │  │  Student 1   │  │  Student 2   │
│              │  │              │  │              │
│ 🎤 Mic: ON   │  │ 🎤 Mic: ON   │  │ 🎤 Mic: OFF  │
│ 🔊 Speaker   │  │ 🔊 Speaker   │  │ 🔊 Speaker   │
└──────────────┘  └──────────────┘  └──────────────┘
       │                │                 │
       │ Audio Stream   │                 │
       ├───────────────>│                 │
       │                ├────────────────>│
       │<───────────────┤                 │
       │<───────────────┼─────────────────┤ (Listening only)
       
Note: Spring Boot N'EST PAS dans ce flux !
```

## Modèle de Données Relationnel

```
┌─────────────────────────────────────┐
│             users                   │
├─────────────────────────────────────┤
│ PK  id                 BIGINT      │
│     name               VARCHAR     │
│ UQ  email              VARCHAR     │
│     password_hash      VARCHAR     │
│     role               ENUM         │◄───────┐
│     created_at         TIMESTAMP   │        │
└─────────────────────────────────────┘        │
                                               │
                                               │ FK teacher_id
┌─────────────────────────────────────┐        │
│         live_classes                │        │
├─────────────────────────────────────┤        │
│ PK  id                 BIGINT      │        │
│     title              VARCHAR     │        │
│ FK  teacher_id         BIGINT      │────────┘
│     status             ENUM         │
│     scheduled_at       TIMESTAMP   │
│     started_at         TIMESTAMP   │
│     ended_at           TIMESTAMP   │
│     created_at         TIMESTAMP   │
└────────┬────────────────────────────┘
         │
         │ FK live_class_id
         │
┌────────▼────────────────────────────┐
│      live_class_students            │
├─────────────────────────────────────┤
│ PK,FK live_class_id    BIGINT      │────┐
│ PK,FK student_id       BIGINT      │◄───┼──┐
└─────────────────────────────────────┘    │  │
         │                                 │  │
         │ FK live_class_id                │  │ FK student_id
         │                                 │  │
┌────────▼────────────────────────────┐    │  │
│          attendance                 │    │  │
├─────────────────────────────────────┤    │  │
│ PK  id                 BIGINT      │    │  │
│ FK  live_class_id      BIGINT      │────┘  │
│ FK  student_id         BIGINT      │◄──────┘
│     joined_at          TIMESTAMP   │
│     left_at            TIMESTAMP   │
└─────────────────────────────────────┘
```

## États d'une Classe en Direct

```
┌───────────────┐
│   SCHEDULED   │  Classe créée
└───────┬───────┘
        │
        │ POST /live-classes/{id}/start
        │ (by teacher)
        ▼
┌───────────────┐
│     LIVE      │  Classe en cours
└───────┬───────┘  • Participants peuvent rejoindre
        │          • Audio actif
        │          • Tokens LiveKit générés
        │
        │ POST /live-classes/{id}/end
        │ (by teacher)
        ▼
┌───────────────┐
│     ENDED     │  Classe terminée
└───────────────┘  • Plus de nouvelles connexions
                   • Attendance records finalisés
```

## Architecture en Couches

```
┌─────────────────────────────────────────────────────────┐
│                   PRESENTATION LAYER                    │
│  ┌───────────────────────────────────────────────────┐ │
│  │             REST Controllers                      │ │
│  │  @RestController                                  │ │
│  │  • UserController                                 │ │
│  │  • LiveClassController                            │ │
│  │  • HealthController                               │ │
│  └───────────────────────────────────────────────────┘ │
└────────────────────┬────────────────────────────────────┘
                     │
                     │ DTOs
                     │
┌────────────────────▼────────────────────────────────────┐
│                    SERVICE LAYER                        │
│  ┌───────────────────────────────────────────────────┐ │
│  │           Business Logic Services                 │ │
│  │  @Service @Transactional                          │ │
│  │  • UserService                                    │ │
│  │  • LiveClassService                               │ │
│  │  • LiveKitService                                 │ │
│  └───────────────────────────────────────────────────┘ │
└────────────────────┬────────────────────────────────────┘
                     │
                     │ Entities
                     │
┌────────────────────▼────────────────────────────────────┐
│                 PERSISTENCE LAYER                       │
│  ┌───────────────────────────────────────────────────┐ │
│  │           Spring Data Repositories                │ │
│  │  @Repository                                      │ │
│  │  • UserRepository                                 │ │
│  │  • LiveClassRepository                            │ │
│  │  • LiveClassStudentRepository                     │ │
│  │  • AttendanceRepository                           │ │
│  └───────────────────────────────────────────────────┘ │
└────────────────────┬────────────────────────────────────┘
                     │
                     │ JDBC
                     │
┌────────────────────▼────────────────────────────────────┐
│                     DATABASE                            │
│               PostgreSQL 16                             │
└─────────────────────────────────────────────────────────┘
```

## Conteneurs Docker

```
┌─────────────────────────────────────────────────────────┐
│              DOCKER HOST (macOS)                        │
│                                                         │
│  ┌───────────────────────────────────────────────────┐ │
│  │         Docker Network: micraa-network            │ │
│  │                                                   │ │
│  │  ┌──────────────────────────────────────────┐   │ │
│  │  │  Container: micraa-app                   │   │ │
│  │  │  Image: custom (Dockerfile)              │   │ │
│  │  │  Ports: 8080:8080                        │   │ │
│  │  │  ┌────────────────────────────────────┐ │   │ │
│  │  │  │  Spring Boot Application           │ │   │ │
│  │  │  │  Java 21 + Maven                   │ │   │ │
│  │  │  └────────────────────────────────────┘ │   │ │
│  │  └──────────────┬───────────────────────────┘   │ │
│  │                 │                               │ │
│  │  ┌──────────────▼───────────────────────────┐   │ │
│  │  │  Container: micraa-postgres              │   │ │
│  │  │  Image: postgres:16-alpine               │   │ │
│  │  │  Ports: 5432:5432                        │   │ │
│  │  │  Volume: postgres_data                   │   │ │
│  │  │  ┌────────────────────────────────────┐ │   │ │
│  │  │  │  PostgreSQL Database               │ │   │ │
│  │  │  │  DB: micraa                        │ │   │ │
│  │  │  └────────────────────────────────────┘ │   │ │
│  │  └──────────────────────────────────────────┘   │ │
│  │                                                   │ │
│  │  ┌──────────────────────────────────────────┐   │ │
│  │  │  Container: micraa-livekit               │   │ │
│  │  │  Image: livekit/livekit-server:latest    │   │ │
│  │  │  Ports: 7880, 7881, 50000-50100/udp     │   │ │
│  │  │  ┌────────────────────────────────────┐ │   │ │
│  │  │  │  LiveKit Server                    │ │   │ │
│  │  │  │  WebRTC + Signaling                │ │   │ │
│  │  │  └────────────────────────────────────┘ │   │ │
│  │  └──────────────────────────────────────────┘   │ │
│  │                                                   │ │
│  └───────────────────────────────────────────────────┘ │
│                                                         │
└─────────────────────────────────────────────────────────┘
         │                                         │
         │ Port Mapping                            │
         ▼                                         ▼
    localhost:8080                           localhost:7880
    (API REST)                               (LiveKit WebRTC)
```

## Cycle de Vie d'une Session de Classe

```
1. PRÉPARATION
┌─────────────────────────────────────────────┐
│ Teacher creates class                       │
│ POST /api/live-classes                      │
│ Status: SCHEDULED                           │
└────────────────┬────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────┐
│ Teacher adds students                       │
│ POST /api/live-classes/{id}/students        │
└────────────────┬────────────────────────────┘

2. DÉMARRAGE
                 │
                 ▼
┌─────────────────────────────────────────────┐
│ Teacher starts class                        │
│ POST /api/live-classes/{id}/start           │
│ Status: SCHEDULED → LIVE                    │
│ started_at = now()                          │
└────────────────┬────────────────────────────┘

3. CONNEXION DES PARTICIPANTS
                 │
                 ▼
┌─────────────────────────────────────────────┐
│ Teacher joins                               │
│ POST /api/live-classes/{id}/join?userId=1   │
│ → Get LiveKit token                         │
│ → Connect to LiveKit                        │
└────────────────┬────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────┐
│ Students join                               │
│ POST /api/live-classes/{id}/join?userId=2   │
│ → Verify authorization                      │
│ → Get LiveKit token                         │
│ → Create attendance record                  │
│ → Connect to LiveKit                        │
└────────────────┬────────────────────────────┘

4. SESSION ACTIVE
                 │
                 ▼
┌─────────────────────────────────────────────┐
│ Real-time audio communication               │
│ • Teacher speaks → all students hear        │
│ • Students speak → all hear                 │
│ • Mute/unmute controls                      │
│ • Participant list updates                  │
│ • Chat messages                             │
│                                             │
│ (Audio flows through LiveKit, not API)      │
└────────────────┬────────────────────────────┘

5. FIN DE SESSION
                 │
                 ▼
┌─────────────────────────────────────────────┐
│ Teacher ends class                          │
│ POST /api/live-classes/{id}/end             │
│ Status: LIVE → ENDED                        │
│ ended_at = now()                            │
│ Update attendance records (left_at)         │
└────────────────┬────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────┐
│ Participants disconnected                   │
│ Room closed                                 │
│ Data persisted                              │
└─────────────────────────────────────────────┘
```

## Sécurité : Génération de Token LiveKit

```
┌────────────────────────────────────────────────────┐
│           LiveKit Token Structure                  │
├────────────────────────────────────────────────────┤
│                                                    │
│  JWT Header:                                       │
│  {                                                 │
│    "alg": "HS256",                                 │
│    "typ": "JWT"                                    │
│  }                                                 │
│                                                    │
│  JWT Payload:                                      │
│  {                                                 │
│    "video": {                                      │
│      "room": "class-1",              ◄─ Room name │
│      "roomJoin": true,               ◄─ Can join  │
│      "canPublish": true,             ◄─ Can speak │
│      "canSubscribe": true            ◄─ Can hear  │
│    },                                              │
│    "name": "Alice Dupont",           ◄─ Display   │
│    "identity": "student-2",          ◄─ Unique ID │
│    "exp": 1724281200                 ◄─ Expiry    │
│  }                                                 │
│                                                    │
│  JWT Signature:                                    │
│  HMACSHA256(                                       │
│    base64UrlEncode(header) + "." +                 │
│    base64UrlEncode(payload),                       │
│    "secret"  ◄─────────── API Secret (SERVER ONLY)│
│  )                                                 │
│                                                    │
└────────────────────────────────────────────────────┘

⚠️  Le secret API ne doit JAMAIS être exposé au client !
✅  Les tokens sont générés uniquement côté serveur
✅  Les tokens ont une durée de vie limitée (6h)
✅  Les permissions sont définies dans le token
```

## Déploiement

```
Development:
┌──────────────────────────────────────┐
│  macOS (Docker Desktop)              │
│  ┌────────────────────────────────┐ │
│  │  docker-compose up             │ │
│  │  • PostgreSQL                  │ │
│  │  • LiveKit                     │ │
│  │  • Spring Boot                 │ │
│  └────────────────────────────────┘ │
└──────────────────────────────────────┘

Production (Future):
┌──────────────────────────────────────┐
│  Cloud Infrastructure                │
│  ┌────────────────────────────────┐ │
│  │  Kubernetes / Docker Swarm     │ │
│  │  • Load Balancer               │ │
│  │  • App instances (scale)       │ │
│  │  • PostgreSQL (managed)        │ │
│  │  • LiveKit (cluster)           │ │
│  │  • Redis (cache)               │ │
│  │  • Monitoring (Prometheus)     │ │
│  └────────────────────────────────┘ │
└──────────────────────────────────────┘
```
