# Makefile pour Micraa

.PHONY: help build up down logs test clean restart

help: ## Afficher l'aide
	@echo "Commandes disponibles :"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2}'

build: ## Construire les images Docker
	docker-compose build

up: ## Démarrer tous les services
	docker-compose up -d
	@echo "✅ Services démarrés !"
	@echo "📊 Spring Boot : http://localhost:8080"
	@echo "🗄️  PostgreSQL  : localhost:5432"
	@echo "🎙️  LiveKit     : ws://localhost:7880"

down: ## Arrêter tous les services
	docker-compose down

logs: ## Voir les logs de tous les services
	docker-compose logs -f

logs-app: ## Voir les logs de l'application
	docker-compose logs -f app

logs-db: ## Voir les logs de PostgreSQL
	docker-compose logs -f postgres

logs-livekit: ## Voir les logs de LiveKit
	docker-compose logs -f livekit

test: ## Exécuter le script de test
	@echo "⏳ Attente du démarrage de l'application..."
	@sleep 5
	./test-api.sh

clean: ## Arrêter et supprimer tous les volumes
	docker-compose down -v

restart: down up ## Redémarrer tous les services

rebuild: clean build up ## Reconstruire complètement

health: ## Vérifier la santé de l'API
	@curl -s http://localhost:8080/api/health | jq .

db-shell: ## Accéder au shell PostgreSQL
	docker exec -it micraa-postgres psql -U micraa -d micraa

db-tables: ## Lister les tables de la base de données
	docker exec -it micraa-postgres psql -U micraa -d micraa -c "\dt"

run-local: ## Démarrer l'application en local (sans Docker)
	./mvnw spring-boot:run -Dspring-boot.run.profiles=local

mvn-clean: ## Nettoyer le build Maven
	./mvnw clean

mvn-package: ## Créer le package JAR
	./mvnw clean package -DskipTests

docker-start: ## Démarrer uniquement PostgreSQL et LiveKit
	docker run --name micraa-postgres -e POSTGRES_DB=micraa -e POSTGRES_USER=micraa -e POSTGRES_PASSWORD=micraa123 -p 5432:5432 -d postgres:16-alpine || echo "PostgreSQL déjà démarré"
	docker run --name micraa-livekit -p 7880:7880 -p 7881:7881 -p 50000-50100:50000-50100/udp -e LIVEKIT_KEYS="devkey: secret" -d livekit/livekit-server:latest --dev --bind 0.0.0.0 || echo "LiveKit déjà démarré"

docker-stop: ## Arrêter PostgreSQL et LiveKit individuels
	docker stop micraa-postgres micraa-livekit || true
	docker rm micraa-postgres micraa-livekit || true
