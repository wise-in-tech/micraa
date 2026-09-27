# Exigences Fonctionnelles - Micraa

## Objectif

Micraa est une plateforme de classes en direct pour établissements scolaires. La première version fournit des classes audio en temps réel, avec authentification, gestion des utilisateurs, planification des classes et suivi de présence.

Pour l'architecture technique, consulter [skills/ARCHITECTURE.md](skills/ARCHITECTURE.md). Pour le déploiement, consulter [operations/DOCKER.md](operations/DOCKER.md).

## Rôles et permissions

Un utilisateur peut posséder un ou plusieurs rôles. Les permissions sont cumulatives.

| Rôle | Permissions |
|------|-------------|
| `ADMIN` | Gérer la plateforme, créer les comptes enseignants, créer et planifier les classes, attribuer un enseignant à une classe |
| `TEACHER` | Gérer, démarrer et terminer les classes qui lui sont attribuées; participer à une classe en audio; couper ou réactiver le microphone d'un étudiant dans sa classe |
| `STUDENT` | Créer son compte en ligne, consulter les classes autorisées, rejoindre une classe et participer à l'audio |

Règles obligatoires :

- L'inscription publique crée uniquement un compte `STUDENT`.
- Le client ne peut pas choisir son rôle à l'inscription.
- Seul un `ADMIN` peut créer un compte `TEACHER`.
- La création d'un compte `ADMIN` est réservée à l'administration de Micraa.
- Seul un `ADMIN` peut créer et planifier une classe.
- Un `TEACHER` ne peut gérer que les classes qui lui sont attribuées.
- Un `STUDENT` ne peut pas créer, planifier, démarrer ou terminer une classe.
- Seul l'enseignant assigné peut couper ou réactiver le microphone d'un étudiant dans une classe active.
- Un étudiant ne peut pas contrôler le microphone d'un autre participant.
- Les permissions doivent être vérifiées côté backend sur chaque endpoint protégé.

## Authentification et mots de passe

- `POST /api/auth/register` crée un étudiant et retourne un JWT.
- `POST /api/auth/login` authentifie un utilisateur et retourne un JWT.
- Un utilisateur authentifié peut modifier son mot de passe en fournissant son ancien mot de passe.
- Un utilisateur peut demander une réinitialisation avec son adresse e-mail.
- Le lien de réinitialisation est à usage unique, signé ou aléatoire, stocké sous forme hachée et limité dans le temps.
- La réponse à `forgot-password` doit rester générique afin de ne pas révéler si une adresse existe.
- Les e-mails sont envoyés depuis une adresse Google dédiée de type `no-reply`.
- L'adresse et les paramètres SMTP sont fournis par variables d'environnement et ne sont jamais commités.

## Contrat API cible

| Méthode | Endpoint | Accès | Résultat attendu |
|---------|----------|-------|------------------|
| `POST` | `/api/auth/register` | Public | Crée uniquement un `STUDENT` |
| `POST` | `/api/auth/login` | Public | Retourne un JWT |
| `POST` | `/api/auth/change-password` | Authentifié | Change le mot de passe |
| `POST` | `/api/auth/forgot-password` | Public | Envoie un lien sans révéler l'existence du compte |
| `POST` | `/api/auth/reset-password` | Token valide | Définit un nouveau mot de passe |
| `POST` | `/api/admin/teachers` | `ADMIN` | Crée un enseignant |
| `POST` | `/api/live-classes` | `ADMIN` | Crée et planifie une classe |
| `POST` | `/api/live-classes/{id}/assign-teacher` | `ADMIN` | Attribue un enseignant |
| `POST` | `/api/live-classes/{id}/start` | Enseignant assigné | Démarre la classe |
| `POST` | `/api/live-classes/{id}/end` | Enseignant assigné | Termine la classe |
| `POST` | `/api/live-classes/{id}/join` | Participant autorisé | Retourne le token LiveKit |

## Modèle fonctionnel cible

### Utilisateur

- `id`
- `name`
- `email` unique
- `passwordHash`
- un ou plusieurs rôles
- `createdAt`

### Classe en direct

- `id`
- `title`
- `teacherId`
- `status`: `SCHEDULED`, `LIVE`, `ENDED`
- `scheduledAt`
- `startedAt`
- `endedAt`
- `createdAt`

### Réinitialisation de mot de passe

- `id`
- `userId`
- `tokenHash`
- `expiresAt`
- `usedAt`
- `createdAt`

## Critères d'acceptation de la prochaine implémentation

- Une inscription avec `role=TEACHER` crée quand même un `STUDENT`.
- Un étudiant reçoit `403 Forbidden` pour les endpoints admin et teacher.
- Un enseignant reçoit `403 Forbidden` pour créer une classe.
- Un enseignant non assigné reçoit `403 Forbidden` pour démarrer ou terminer une classe.
- Un admin peut créer un enseignant et planifier une classe.
- Un utilisateur peut changer son mot de passe avec son ancien mot de passe.
- Un lien de réinitialisation expire et ne peut être utilisé qu'une fois.
- Les tests couvrent chaque règle du tableau des permissions.

## État actuel du MVP

Depuis ticket-001, `ADMIN` existe dans l'énumération des rôles mais un utilisateur ne stocke encore qu'un seul rôle. L'inscription publique ignore désormais tout `role` transmis par le client et crée toujours un `STUDENT`. Seul un `ADMIN` peut créer un `TEACHER`, via `POST /api/admin/teachers` ; aucun endpoint ne peut créer un `ADMIN` (le premier compte est inséré par script SQL, hors de l'application). Le flux e-mail de réinitialisation de mot de passe n'est pas encore implémenté, et la création/planification des classes n'est pas encore réservée à `ADMIN`. Ce document décrit la cible fonctionnelle complète à implémenter.

## Feuille de route V2 : intégration Moodle

Moodle est prévu dans la version 2 et ne fait pas partie du périmètre d'implémentation V1.

La V2 pourra couvrir :

- l'association des utilisateurs Micraa avec les utilisateurs Moodle ;
- la synchronisation des cours, groupes et inscriptions nécessaires ;
- l'accès à une classe Micraa depuis un cours Moodle ;
- la remontée contrôlée du statut de classe et des présences vers Moodle ;
- une stratégie d'authentification ou de SSO compatible avec Moodle.

Avant le développement V2, il faudra décider : la version Moodle supportée, le plugin ou l'API REST utilisés, le sens de synchronisation, la propriété des données, la résolution des conflits et la gestion des erreurs/reprises.

Contraintes à respecter dès la V1 : Moodle ne doit pas être une dépendance du cœur métier, les identifiants Moodle ne doivent pas remplacer les identifiants Micraa, les secrets Moodle ne doivent jamais atteindre Flutter et les synchronisations devront être idempotentes et traçables.
