# Documentation Micraa

Point d'entrée unique de la documentation du projet.

## Fonctionnel

- [Exigences fonctionnelles](FUNCTIONAL-REQUIREMENTS.md) : rôles, permissions, comptes, classes, mots de passe, e-mails et critères d'acceptation.
- [Development Skill](skills/school-live-classroom-development-skill.md) : principes produit et règles de développement de la première version.

## Technique

- [Architecture technique](skills/ARCHITECTURE.md) : composants, flux, modèle technique, sécurité et exposition HTTPS.
- [Diagrammes](../DIAGRAMS.md) : architecture, données et cycle de vie des classes.
- [Intégration Flutter](../FLUTTER-INTEGRATION.md) : configuration du client mobile et LiveKit.

## Opérations

- [Docker et déploiement](operations/DOCKER.md) : démarrage, configuration, tests API, logs et dépannage.
- [Démarrage rapide](operations/QUICK-START.md) : procédure courte pour lancer le projet.
- [Makefile](../Makefile) : commandes locales disponibles.

## Tests et API

- [Collection Postman](../Micraa-API.postman_collection.json)
- [Script de test API](../test-api.sh)

## Tickets d'implémentation

- [Ticket 001 - Immediate V1 improvements](tickets/ticket-001.md)
- [Ticket 002 - Moodle V2 integration](tickets/ticket-002.md)

## Parcours recommandé

1. Lire les [exigences fonctionnelles](FUNCTIONAL-REQUIREMENTS.md).
2. Lire l'[architecture technique](skills/ARCHITECTURE.md).
3. Suivre le [guide Docker](operations/DOCKER.md).
4. Consulter l'[intégration Flutter](../FLUTTER-INTEGRATION.md) pour le client mobile.

Les documents fonctionnels définissent le comportement attendu. Les documents techniques expliquent la solution. Le guide opérations explique comment l'exécuter et la déployer.
