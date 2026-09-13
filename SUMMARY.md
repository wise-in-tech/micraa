# 🎓 Micraa - Plateforme de Classes en Direct

## ✅ Résumé de l'Implémentation

### Ce qui a été développé

Nous avons créé une **plateforme complète de classes en direct** basée sur les exigences du document `school-live-classroom-development-skill.md`, avec :

#### 🏗️ Architecture
- ✅ Backend Spring Boot (Java 21)
- ✅ Base de données PostgreSQL
- ✅ Intégration LiveKit pour l'audio en temps réel
- ✅ Configuration Docker Compose complète

#### 📊 Modèle de Données
- ✅ `User` (enseignants et étudiants)
- ✅ `LiveClass` (classes en direct avec statuts)
- ✅ `LiveClassStudent` (inscription des étudiants)
- ✅ `Attendance` (suivi de présence)

#### 🔌 API REST
- ✅ Gestion des utilisateurs
- ✅ Gestion des classes en direct
- ✅ Génération des tokens LiveKit sécurisés
- ✅ Contrôle d'accès (enseignants/étudiants)

#### 🐳 Infrastructure
- ✅ Dockerfile optimisé (multi-stage build)
- ✅ Docker Compose avec 3 services (app, postgres, livekit)
- ✅ Configuration pour développement local

#### 📚 Documentation
- ✅ `README-DOCKER.md` - Guide complet Docker
- ✅ `QUICK-START.md` - Démarrage rapide
- ✅ `ARCHITECTURE.md` - Architecture détaillée
- ✅ `FLUTTER-INTEGRATION.md` - Guide d'intégration mobile
- ✅ Collection Postman pour tests API

#### 🧪 Outils de Test
- ✅ Script de test automatique (`test-api.sh`)
- ✅ Makefile avec commandes utiles
- ✅ Collection Postman

---

## 🚀 Démarrage Rapide

### Prérequis
1. **Démarrer Docker Desktop** sur votre Mac

### Lancer l'Application

```bash
cd /Users/mohy/sofiane-devenv/micraa

# Option 1: Avec Docker Compose (recommandé)
docker-compose up --build

# Option 2: Avec Make
make build
make up

# Attendre que tous les services soient démarrés...
```

### Vérifier que ça fonctionne

```bash
# Vérifier l'API
curl http://localhost:8080/api/health

# Résultat attendu:
# {"status":"UP","application":"micraa"}
```

### Tester l'API

```bash
# Exécuter le script de test complet
./test-api.sh
```

Ce script va :
1. Créer un enseignant et deux étudiants
2. Créer une classe en direct
3. Ajouter les étudiants à la classe
4. Démarrer la classe
5. Générer les tokens LiveKit pour tous les participants
6. Afficher toutes les informations de connexion

---

## 📦 Services Déployés

Une fois démarré, vous avez accès à :

| Service | URL | Description |
|---------|-----|-------------|
| 🌐 API REST | http://localhost:8080/api | Backend Spring Boot |
| 🗄️ PostgreSQL | localhost:5432 | Base de données |
| 🎙️ LiveKit | ws://localhost:7880 | Serveur WebRTC audio |

---

## 🧪 Tests Manuels avec cURL

### 1. Créer un enseignant
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Prof. Durand","email":"durand@school.com","role":"TEACHER"}'
```

### 2. Créer un étudiant
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Sophie Martin","email":"sophie@school.com","role":"STUDENT"}'
```

### 3. Créer une classe
```bash
curl -X POST http://localhost:8080/api/live-classes \
  -H "Content-Type: application/json" \
  -d '{"title":"Cours de Mathématiques","teacherId":1}'
```

### 4. Ajouter des étudiants à la classe
```bash
curl -X POST http://localhost:8080/api/live-classes/1/students \
  -H "Content-Type: application/json" \
  -d '{"studentIds":[2]}'
```

### 5. Démarrer la classe (enseignant)
```bash
curl -X POST "http://localhost:8080/api/live-classes/1/start?teacherId=1"
```

### 6. Rejoindre la classe (obtenir le token LiveKit)
```bash
# Enseignant
curl -X POST "http://localhost:8080/api/live-classes/1/join?userId=1"

# Étudiant
curl -X POST "http://localhost:8080/api/live-classes/1/join?userId=2"
```

Le résultat contient :
- `livekitUrl` : URL du serveur LiveKit
- `accessToken` : Token JWT à utiliser pour la connexion
- `roomName` : Nom de la salle (ex: `class-1`)

---

## 📱 Tests Audio avec Application Mobile

### Prochaine Étape : Application Flutter

Pour tester l'audio en temps réel, vous devez :

1. **Développer l'application Flutter** en suivant `FLUTTER-INTEGRATION.md`
2. **Utiliser les tokens générés** par l'API `/join` 
3. **Connecter le client LiveKit** avec ces tokens
4. **Tester l'audio bidirectionnel** entre plusieurs devices

### Exemple de Flux
```
1. Device 1 (Enseignant) -> API /join -> Token A -> LiveKit
2. Device 2 (Étudiant)   -> API /join -> Token B -> LiveKit
3. Audio flows: Device 1 <-> LiveKit <-> Device 2
```

---

## 🛠️ Commandes Utiles

### Docker Compose

```bash
# Démarrer
docker-compose up -d

# Voir les logs
docker-compose logs -f

# Arrêter
docker-compose down

# Réinitialiser complètement
docker-compose down -v
```

### Makefile (Plus Simple)

```bash
make help          # Liste toutes les commandes
make up            # Démarrer les services
make logs          # Voir les logs
make test          # Exécuter les tests
make health        # Vérifier l'API
make down          # Arrêter
make clean         # Nettoyer complètement
```

### Base de Données

```bash
# Accéder au shell PostgreSQL
docker exec -it micraa-postgres psql -U micraa -d micraa

# Dans psql:
\dt                           # Lister les tables
SELECT * FROM users;          # Voir les utilisateurs
SELECT * FROM live_classes;   # Voir les classes
\q                            # Quitter
```

---

## 📖 Documentation Complète

- **`QUICK-START.md`** : Guide de démarrage rapide
- **`README-DOCKER.md`** : Documentation Docker détaillée
- **`ARCHITECTURE.md`** : Architecture technique complète
- **`FLUTTER-INTEGRATION.md`** : Guide d'intégration Flutter
- **`Micraa-API.postman_collection.json`** : Collection Postman

---

## ✅ Conformité aux Exigences

### Fonctionnalités Implémentées

| Exigence | Statut | Notes |
|----------|--------|-------|
| Enseignant peut créer une classe | ✅ | API `/live-classes` POST |
| Enseignant peut démarrer une classe | ✅ | API `/live-classes/{id}/start` |
| Étudiants peuvent rejoindre | ✅ | API `/live-classes/{id}/join` |
| Génération tokens LiveKit sécurisés | ✅ | Service `LiveKitService` |
| Contrôle d'accès | ✅ | Vérification enseignant/étudiant |
| Audio en temps réel | ✅ | Intégration LiveKit |
| Suivi de présence | ✅ | Table `attendance` |
| Docker pour déploiement | ✅ | Docker Compose |
| PostgreSQL | ✅ | Container postgres |

### Architecture Respectée

✅ **Séparation des responsabilités**
- Spring Boot : Logique métier, auth, tokens
- LiveKit : WebRTC, audio temps réel
- Aucun audio ne transite par Spring Boot

✅ **Sécurité**
- Secret API LiveKit jamais exposé au client
- Tokens générés côté serveur
- Contrôle d'accès par rôle

✅ **Modularité**
- Architecture monolithique modulaire
- Couches séparées (model, repository, service, controller)
- Évolution possible vers microservices

---

## ⚠️ Points Importants

### Sécurité (Phase 1 - Tests Uniquement)

⚠️ **L'authentification JWT n'est PAS implémentée** pour faciliter les tests initiaux.

- Les `userId` sont passés en paramètres de requête
- Les mots de passe ne sont pas hashés
- **NE PAS utiliser en production**

Ceci sera implémenté dans la **Phase 2** après validation de l'audio.

### Webhooks LiveKit

⚠️ **Les webhooks LiveKit ne sont PAS encore implémentés**.

Les événements suivants seront ajoutés plus tard :
- Participant rejoint/quitte
- Audio activé/désactivé
- Salle créée/fermée

---

## 🎯 Prochaines Étapes

### Phase 1 (Actuelle) : MVP Audio ✅
- [x] Architecture de base
- [x] API REST complète
- [x] Intégration LiveKit
- [x] Configuration Docker
- [ ] **Tests audio avec client Flutter** ⏳ EN COURS

### Phase 2 : Sécurité & Auth
- [ ] Authentification JWT
- [ ] Password hashing (BCrypt)
- [ ] Role-Based Access Control
- [ ] HTTPS/TLS

### Phase 3 : Webhooks & Monitoring
- [ ] LiveKit webhooks
- [ ] Métriques (Prometheus)
- [ ] Logging structuré
- [ ] Health checks avancés

### Phase 4 : Features Avancées
- [ ] Chat textuel persistant
- [ ] Enregistrement des sessions
- [ ] Support vidéo
- [ ] Partage d'écran

---

## 🐛 Dépannage

### Docker ne démarre pas
```bash
# Vérifier que Docker Desktop est lancé
docker --version
docker ps
```

### Port déjà utilisé
```bash
# Trouver le processus qui utilise le port 8080
lsof -i :8080

# Arrêter ou changer le port dans application.properties
```

### Erreur de connexion à la BDD
```bash
# Vérifier que PostgreSQL est démarré
docker ps | grep postgres

# Voir les logs
docker-compose logs postgres
```

### API ne répond pas
```bash
# Vérifier les logs de l'application
docker-compose logs -f app

# Vérifier la santé
curl http://localhost:8080/api/health
```

---

## 📊 Structure du Projet

```
micraa/
├── src/main/java/com/wiseintech/micraa/
│   ├── config/              # Configuration (LiveKit, etc.)
│   ├── controller/          # REST Controllers
│   ├── dto/                 # Data Transfer Objects
│   ├── exception/           # Exception Handlers
│   ├── model/               # JPA Entities
│   ├── repository/          # Spring Data Repositories
│   └── service/             # Business Logic
├── src/main/resources/
│   ├── application.properties          # Config principale
│   └── application-local.properties    # Config locale
├── doc/skills/
│   └── school-live-classroom-development-skill.md
├── Dockerfile                          # Build de l'app
├── docker-compose.yml                  # Orchestration
├── Makefile                            # Commandes utiles
├── test-api.sh                         # Script de test
├── QUICK-START.md                      # Démarrage rapide
├── README-DOCKER.md                    # Guide Docker
├── ARCHITECTURE.md                     # Architecture
├── FLUTTER-INTEGRATION.md              # Guide Flutter
├── Micraa-API.postman_collection.json # Tests Postman
└── pom.xml                             # Dépendances Maven
```

---

## 🎉 Conclusion

Vous disposez maintenant d'une **plateforme complète de classes en direct** prête pour les tests audio !

### Pour valider le système :

1. ✅ **Backend démarré** : `docker-compose up`
2. ✅ **API testée** : `./test-api.sh`
3. ⏳ **Application Flutter** : À développer selon `FLUTTER-INTEGRATION.md`
4. ⏳ **Tests audio** : Avec plusieurs devices

### Support

En cas de problème :
1. Consulter `QUICK-START.md`
2. Vérifier les logs : `make logs`
3. Tester l'API : `curl http://localhost:8080/api/health`
4. Vérifier que Docker Desktop est lancé

---

**Développé selon les spécifications du document `school-live-classroom-development-skill.md`**

🚀 **Prêt pour les tests audio en temps réel !**
