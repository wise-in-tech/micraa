# 📚 Documentation Micraa - Index

Bienvenue dans la documentation de la plateforme Micraa !

## 🚀 Par où commencer ?

### Pour démarrer rapidement l'application
👉 **[QUICK-START.md](QUICK-START.md)** - Guide de démarrage rapide (5 min)

### Pour comprendre le projet
👉 **[SUMMARY.md](SUMMARY.md)** - Résumé exécutif du projet

---

## 📖 Documentation Complète

### 1. Démarrage et Configuration

- **[QUICK-START.md](QUICK-START.md)**
  - Démarrage rapide
  - Commandes essentielles
  - Premiers tests

- **[README-DOCKER.md](README-DOCKER.md)**
  - Guide Docker complet
  - Configuration des services
  - Tests de l'API
  - Dépannage

### 2. Architecture et Conception

- **[ARCHITECTURE.md](ARCHITECTURE.md)**
  - Stack technique
  - Architecture applicative
  - Modèle de données
  - Décisions techniques
  - Patterns utilisés
  - Scalabilité

- **[DIAGRAMS.md](DIAGRAMS.md)**
  - Diagrammes d'architecture
  - Flux de données
  - Schéma de base de données
  - Cycle de vie des classes

### 3. Développement

- **[FLUTTER-INTEGRATION.md](FLUTTER-INTEGRATION.md)**
  - Intégration mobile Flutter
  - Configuration LiveKit
  - Exemples de code
  - Tests audio
  - Dépannage client

### 4. Outils et Tests

- **[Makefile](Makefile)**
  - Commandes make disponibles
  - Raccourcis utiles

- **[test-api.sh](test-api.sh)**
  - Script de test automatique
  - Scénario complet

- **[Micraa-API.postman_collection.json](Micraa-API.postman_collection.json)**
  - Collection Postman
  - Tests API interactifs

### 5. Exigences Métier

- **[doc/skills/school-live-classroom-development-skill.md](doc/skills/school-live-classroom-development-skill.md)**
  - Spécifications complètes
  - Exigences fonctionnelles
  - Architecture recommandée

---

## 🎯 Par Cas d'Usage

### Je veux démarrer l'application pour la première fois
1. Lire [QUICK-START.md](QUICK-START.md)
2. Exécuter `docker-compose up --build`
3. Tester avec `./test-api.sh`

### Je veux comprendre l'architecture
1. Lire [SUMMARY.md](SUMMARY.md) pour la vue d'ensemble
2. Consulter [ARCHITECTURE.md](ARCHITECTURE.md) pour les détails
3. Voir [DIAGRAMS.md](DIAGRAMS.md) pour les visuels

### Je veux développer l'application mobile
1. Lire [FLUTTER-INTEGRATION.md](FLUTTER-INTEGRATION.md)
2. Configurer les dépendances Flutter
3. Implémenter les services API et LiveKit
4. Tester l'audio

### Je veux tester l'API
1. Démarrer l'application : `make up`
2. Utiliser le script : `./test-api.sh`
3. Ou importer la collection Postman
4. Ou utiliser cURL (exemples dans README-DOCKER.md)

### J'ai un problème
1. Consulter la section "Dépannage" dans [QUICK-START.md](QUICK-START.md)
2. Vérifier les logs : `make logs`
3. Consulter [README-DOCKER.md](README-DOCKER.md) section "Dépannage"

---

## 📊 Structure du Projet

```
micraa/
├── 📄 Documentation
│   ├── SUMMARY.md                    ← Résumé exécutif
│   ├── QUICK-START.md                ← Démarrage rapide
│   ├── README-DOCKER.md              ← Guide Docker
│   ├── ARCHITECTURE.md               ← Architecture technique
│   ├── DIAGRAMS.md                   ← Diagrammes visuels
│   ├── FLUTTER-INTEGRATION.md        ← Guide mobile
│   └── INDEX.md                      ← Ce fichier
│
├── 🔧 Configuration
│   ├── docker-compose.yml            ← Orchestration Docker
│   ├── Dockerfile                    ← Build de l'app
│   ├── pom.xml                       ← Dépendances Maven
│   ├── Makefile                      ← Commandes utiles
│   └── application.properties        ← Config Spring Boot
│
├── 🧪 Tests
│   ├── test-api.sh                   ← Script de test
│   └── Micraa-API.postman_collection.json
│
├── 💻 Code Source
│   └── src/main/java/com/wiseintech/micraa/
│       ├── config/                   ← Configuration
│       ├── controller/               ← REST Controllers
│       ├── dto/                      ← Data Transfer Objects
│       ├── exception/                ← Exception Handlers
│       ├── model/                    ← JPA Entities
│       ├── repository/               ← Spring Data Repos
│       └── service/                  ← Business Logic
│
└── 📋 Spécifications
    └── doc/skills/school-live-classroom-development-skill.md
```

---

## 🔑 Concepts Clés

### Séparation des Responsabilités

```
Spring Boot          ←→  LiveKit
(Business Logic)         (Real-time Audio)

✅ Authentication        ✅ WebRTC
✅ Authorization         ✅ Audio Routing
✅ User Management       ✅ Participants
✅ Class Management      ✅ Chat
✅ Token Generation      ✅ Connection Handling
✅ Attendance            
✅ Database              
```

### Workflow Principal

```
1. Enseignant crée une classe
2. Enseignant ajoute des étudiants
3. Enseignant démarre la classe
4. Enseignant/Étudiants rejoignent (obtiennent token LiveKit)
5. Audio en temps réel via LiveKit
6. Enseignant termine la classe
```

---

## 🔗 API Endpoints Principaux

| Endpoint | Méthode | Description |
|----------|---------|-------------|
| `/api/health` | GET | Health check |
| `/api/users` | POST | Créer utilisateur |
| `/api/live-classes` | POST | Créer classe |
| `/api/live-classes/{id}/students` | POST | Ajouter étudiants |
| `/api/live-classes/{id}/start` | POST | Démarrer classe |
| `/api/live-classes/{id}/join` | POST | Rejoindre (get token) |
| `/api/live-classes/{id}/end` | POST | Terminer classe |

📖 Détails complets dans [README-DOCKER.md](README-DOCKER.md)

---

## 🛠️ Commandes Rapides

```bash
# Démarrer
make up              # ou docker-compose up -d

# Voir les logs
make logs            # ou docker-compose logs -f

# Tester
make test            # ou ./test-api.sh

# Santé
make health          # ou curl http://localhost:8080/api/health

# Arrêter
make down            # ou docker-compose down

# Nettoyer
make clean           # ou docker-compose down -v
```

---

## 📞 Support

### En cas de problème

1. **Vérifier Docker Desktop**
   ```bash
   docker --version
   docker ps
   ```

2. **Consulter les logs**
   ```bash
   make logs
   # ou
   docker-compose logs -f app
   ```

3. **Vérifier l'API**
   ```bash
   curl http://localhost:8080/api/health
   ```

4. **Consulter la documentation**
   - [QUICK-START.md](QUICK-START.md) - Section Dépannage
   - [README-DOCKER.md](README-DOCKER.md) - Section Support

---

## 📈 Roadmap

### ✅ Phase 1 : MVP Audio (Actuelle)
- [x] Architecture de base
- [x] API REST
- [x] Intégration LiveKit
- [x] Configuration Docker
- [ ] Tests audio avec Flutter

### 🔄 Phase 2 : Sécurité
- [ ] Authentification JWT
- [ ] Password hashing
- [ ] RBAC
- [ ] HTTPS

### 🔄 Phase 3 : Webhooks
- [ ] LiveKit webhooks
- [ ] Monitoring
- [ ] Métriques

### 🔄 Phase 4 : Features Avancées
- [ ] Chat persistant
- [ ] Enregistrement
- [ ] Vidéo
- [ ] Screen sharing

---

## 🎓 Ressources d'Apprentissage

### Spring Boot
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)

### LiveKit
- [LiveKit Documentation](https://docs.livekit.io/)
- [LiveKit Flutter SDK](https://docs.livekit.io/client-sdk-flutter/)
- [LiveKit Server SDK Java](https://github.com/livekit/server-sdk-java)

### Docker
- [Docker Documentation](https://docs.docker.com/)
- [Docker Compose](https://docs.docker.com/compose/)

### Flutter
- [Flutter Documentation](https://flutter.dev/docs)
- [Flutter WebRTC](https://pub.dev/packages/flutter_webrtc)

---

## 📝 Changelog

### v0.1.0 (2026-08-21)
- ✅ Architecture initiale
- ✅ API REST complète
- ✅ Intégration LiveKit
- ✅ Configuration Docker
- ✅ Documentation complète
- ✅ Scripts de test

---

## 👥 Contributeurs

Ce projet suit les spécifications du document :
`doc/skills/school-live-classroom-development-skill.md`

---

## 📄 Licence

[À définir]

---

**🚀 Prêt à démarrer ? Consultez [QUICK-START.md](QUICK-START.md) !**
