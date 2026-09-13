# Guide de Démarrage Rapide - Micraa

## Étape 1 : Démarrer Docker Desktop

**Avant toute chose, assurez-vous que Docker Desktop est en cours d'exécution sur votre Mac.**

1. Ouvrez Docker Desktop depuis vos Applications
2. Attendez que Docker soit complètement démarré (l'icône dans la barre de menu doit être stable)

## Étape 2 : Démarrer l'application

### Option A : Avec Docker Compose (Recommandé pour les tests)

```bash
# Depuis le répertoire du projet
cd /Users/mohy/sofiane-devenv/micraa

# Construire et démarrer tous les services
docker-compose up --build
```

Cela va démarrer :
- ✅ PostgreSQL (port 5432)
- ✅ LiveKit (port 7880)
- ✅ Application Spring Boot (port 8080)

Pour arrêter : `Ctrl+C` puis `docker-compose down`

### Option B : Services individuels (pour le développement)

#### 1. Démarrer PostgreSQL

```bash
docker run --name micraa-postgres \
  -e POSTGRES_DB=micraa \
  -e POSTGRES_USER=micraa \
  -e POSTGRES_PASSWORD=micraa123 \
  -p 5432:5432 \
  -d postgres:16-alpine
```

#### 2. Démarrer LiveKit

```bash
docker run --name micraa-livekit \
  -p 7880:7880 \
  -p 7881:7881 \
  -p 50000-50100:50000-50100/udp \
  -e LIVEKIT_KEYS="devkey: secret" \
  -d livekit/livekit-server:latest \
  --dev --bind 0.0.0.0
```

#### 3. Démarrer l'application Spring Boot

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

## Étape 3 : Vérifier que tout fonctionne

```bash
# Vérifier l'API
curl http://localhost:8080/api/health

# Devrait retourner: {"status":"UP","application":"micraa"}
```

## Étape 4 : Exécuter les tests

```bash
# Rendre le script exécutable (une seule fois)
chmod +x test-api.sh

# Exécuter les tests
./test-api.sh
```

Ce script va :
1. ✅ Créer un enseignant et deux étudiants
2. ✅ Créer une classe en direct
3. ✅ Ajouter les étudiants à la classe
4. ✅ Démarrer la classe
5. ✅ Générer les tokens LiveKit pour l'enseignant et les étudiants
6. ✅ Afficher les détails de la classe

## Étape 5 : Tester l'audio

Les tokens LiveKit générés par le script peuvent être utilisés dans une application cliente (Flutter) pour tester l'audio en temps réel.

### Informations de connexion LiveKit :
- **URL**: `ws://localhost:7880`
- **Room name**: `class-{id}` (ex: `class-1`)
- **Access token**: Généré par l'API lors du `/join`

## Commandes Utiles

### Voir les logs

```bash
# Tous les services
docker-compose logs -f

# Application seulement
docker-compose logs -f app

# PostgreSQL seulement
docker-compose logs -f postgres

# LiveKit seulement
docker-compose logs -f livekit
```

### Voir les containers en cours d'exécution

```bash
docker ps
```

### Arrêter et nettoyer

```bash
# Arrêter tous les services
docker-compose down

# Arrêter et supprimer les volumes (réinitialisation complète)
docker-compose down -v
```

### Accéder à la base de données

```bash
# Via Docker
docker exec -it micraa-postgres psql -U micraa -d micraa

# Dans psql:
\dt              # Lister les tables
SELECT * FROM users;
SELECT * FROM live_classes;
\q               # Quitter
```

## Exemples de requêtes API

### Créer un utilisateur

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Jean Dupont",
    "email": "jean@school.com",
    "role": "TEACHER"
  }'
```

### Créer une classe

```bash
curl -X POST http://localhost:8080/api/live-classes \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Cours de Physique",
    "teacherId": 1
  }'
```

### Démarrer une classe

```bash
curl -X POST "http://localhost:8080/api/live-classes/1/start?teacherId=1"
```

### Rejoindre une classe

```bash
curl -X POST "http://localhost:8080/api/live-classes/1/join?userId=1"
```

## Dépannage

### Docker ne démarre pas
- Vérifiez que Docker Desktop est en cours d'exécution
- Vérifiez : `docker --version`

### Port déjà utilisé
Si le port 8080, 5432 ou 7880 est déjà utilisé :
```bash
# Trouver le processus
lsof -i :8080
# Arrêter le processus ou modifier le port dans application.properties
```

### Base de données vide
```bash
# Réinitialiser complètement
docker-compose down -v
docker-compose up --build
```

### Erreur de connexion LiveKit
- Vérifiez que LiveKit est démarré : `docker ps | grep livekit`
- Vérifiez les logs : `docker-compose logs livekit`

## Architecture de Test

```
┌─────────────────┐
│   test-api.sh   │  Script de test automatique
└────────┬────────┘
         │
         v
┌─────────────────┐
│   Spring Boot   │  API REST (port 8080)
│   Application   │
└────────┬────────┘
         │
    ┌────┴────┐
    │         │
    v         v
┌───────┐ ┌────────┐
│ PostgreSQL  │ │ LiveKit│  Communication audio temps réel
│   DB    │ │        │
└───────┘ └────────┘
```

## Prochaines étapes

1. ✅ Vérifier que l'application démarre correctement
2. ✅ Exécuter le script de test
3. ✅ Vérifier les tokens LiveKit générés
4. ⏳ Développer l'application mobile Flutter
5. ⏳ Tester l'audio en temps réel
6. ⏳ Implémenter l'authentification JWT
7. ⏳ Implémenter les webhooks LiveKit

## Support

En cas de problème :
1. Vérifiez que Docker Desktop est démarré
2. Consultez les logs : `docker-compose logs -f`
3. Vérifiez l'API : `curl http://localhost:8080/api/health`
4. Consultez README-DOCKER.md pour plus de détails
