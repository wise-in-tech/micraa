# Ticket 002 - Moodle V2 Integration

## Developer Prompt

You are a senior functional analyst and an expert Java Spring Boot, Spring Security, PostgreSQL, Flutter, and Moodle integration developer.

Work on the existing Micraa codebase. Read these documents first:

1. [Functional requirements](../FUNCTIONAL-REQUIREMENTS.md)
2. [Technical architecture](../skills/ARCHITECTURE.md)
3. [Development skill](../skills/school-live-classroom-development-skill.md)
4. [Ticket 001](ticket-001.md)
5. [Docker operations guide](../operations/DOCKER.md)

This ticket describes the Moodle V2 scope. Do not implement it as part of Ticket 001.

## Core architectural decision

Moodle is the system of record for school administration and learning management. Micraa must not become a second Moodle.

Moodle owns or will own:

- user accounts;
- authentication or SSO;
- roles and role assignments;
- administrators, teachers, and students;
- courses;
- groups and cohorts;
- course enrolments;
- teacher-student relationships;
- course activities;
- course-level class creation and scheduling;
- pedagogical data and general progress.

Micraa owns the live communication capability:

- live-session execution;
- LiveKit rooms and access tokens;
- real-time audio;
- real-time participant state;
- technical attendance events;
- LiveKit webhooks;
- live-session status;
- the integration contract with Moodle.

## V2 objectives

Implement a Moodle integration that allows a Moodle-authorized user to access a Micraa live session without duplicating Moodle account, course, group, or enrolment management.

The integration must support a clear separation between:

- Moodle identity and permissions;
- Micraa live-session authorization;
- LiveKit room access;
- attendance and session events.

## Integration decisions to make first

Before coding, analyze and document the recommended choice for:

- Moodle version support;
- Moodle REST API versus a dedicated Moodle plugin;
- OAuth2/OIDC, Moodle SSO, or another authentication mechanism;
- server-to-server authentication;
- synchronous versus scheduled synchronization;
- Moodle events or webhooks, if available;
- the source of truth for each data field;
- conflict resolution;
- retry and failure handling.

Do not expose Moodle credentials, web-service tokens, or LiveKit secrets to Flutter.

## Identity and role model

Moodle is authoritative for users and roles.

Micraa may keep a local cache or mapping, but it must preserve:

- a stable internal Micraa ID;
- the Moodle provider name;
- the external Moodle user ID;
- the last synchronization timestamp;
- the relevant Moodle role claims or a role snapshot for audit.

Moodle roles must not be replaced by a client-provided role. Micraa must validate the signed token or trusted server-to-server context received from Moodle.

A Moodle user may have multiple roles. Micraa must evaluate the effective permissions required for a live-session action without assuming that a user has only one role.

## Moodle responsibilities

Moodle must remain responsible for:

- creating and managing users;
- assigning administrator, teacher, and student roles;
- creating courses;
- creating groups and cohorts;
- enrolling users;
- creating or authorizing a live-class activity;
- scheduling a class at the course level;
- determining which users are allowed to access the activity.

Micraa must not expose replacement endpoints for these functions in the V2 model.

## Micraa responsibilities

Micraa must be responsible for:

- validating Moodle-originated identity and authorization;
- creating or opening a LiveKit room for an authorized activity;
- generating short-lived LiveKit tokens;
- limiting each token to the correct room and participant identity;
- managing real-time audio and participant state;
- receiving and validating LiveKit webhooks;
- recording technical join/leave times;
- returning agreed session status and attendance data to Moodle.

## Target data model

Use normalized local mappings. Moodle IDs must never become Micraa primary keys.

### ExternalIdentity

- `id`
- `provider` such as `MOODLE`
- `externalUserId`
- `micraaUserId` when a local user cache is required
- `lastSynchronizedAt`
- `createdAt`
- `updatedAt`

### ExternalCourse

- `id`
- `provider`
- `externalCourseId`
- `micraaCourseReference` only if required
- `lastSynchronizedAt`

### ExternalActivity

- `id`
- `provider`
- `externalActivityId`
- `externalCourseId`
- `micraaLiveSessionId`
- `lastSynchronizedAt`

### LiveSession

- `id`
- `externalActivityId`
- `externalCourseId`
- `titleSnapshot`
- `status`: `SCHEDULED`, `LIVE`, `ENDED`
- `scheduledAt`
- `startedAt`
- `endedAt`
- `liveKitRoomName`
- `createdAt`
- `updatedAt`

### LiveSessionParticipant

- `id`
- `liveSessionId`
- `externalUserId`
- `roleSnapshot`
- `joinedAt`
- `leftAt`
- `connectionSequence`

### IntegrationEvent

Use an integration log or equivalent mechanism to record:

- provider;
- external event ID or idempotency key;
- event type;
- processing status;
- retry count;
- error summary without secrets;
- received and processed timestamps.

## Target flows

### Authentication flow

1. The user authenticates with Moodle or the approved SSO mechanism.
2. Moodle or the identity provider issues a verifiable identity assertion.
3. Micraa validates the signature, issuer, audience, expiry, and required claims.
4. Micraa maps the external user to its local identity record.
5. Micraa issues an internal short-lived JWT only if its own API requires one.
6. Flutter stores only the required token using secure platform storage.

### Create or schedule a live activity

1. An authorized Moodle administrator or teacher creates/authorizes the activity in Moodle.
2. Moodle sends the activity, course, schedule, and authorization context to Micraa.
3. Micraa validates the trusted request.
4. Micraa creates or updates the local `LiveSession` mapping idempotently.
5. Micraa returns the session identifier and status to Moodle.

### Join a live session

1. Flutter requests access to the Micraa session.
2. Micraa validates the user identity and Moodle authorization context.
3. Micraa verifies the user is enrolled in the course or group where required.
4. Micraa verifies the live session status.
5. Micraa creates a short-lived LiveKit token for the correct room and identity.
6. Micraa returns only the LiveKit URL, room name, and access token.
7. Flutter connects directly to `wss://livekit.micraa.be`.
8. Micraa records technical presence and processes LiveKit events.

### Attendance synchronization

1. Micraa records technical join and leave events.
2. Micraa calculates the agreed attendance information.
3. Micraa sends or exposes the agreed result to Moodle.
4. Repeated synchronization must not duplicate attendance records.
5. Failed synchronization must be retryable and observable.

## Target API boundary

Moodle owns user, role, course, group, and enrolment administration.

Micraa should expose only integration and live-session endpoints such as:

```text
POST /api/integrations/moodle/sessions
POST /api/integrations/moodle/sessions/{id}/sync
GET  /api/live-sessions/{id}
POST /api/live-sessions/{id}/join
POST /api/live-sessions/{id}/start
POST /api/live-sessions/{id}/end
POST /api/livekit/webhook
```

Refine the API only after the authentication and synchronization design is approved. Document all final request and response DTOs.

## Security requirements

- Validate all Moodle tokens or signed requests.
- Check issuer, audience, expiry, signature, and required claims.
- Never trust roles supplied by Flutter.
- Never send Moodle or LiveKit secrets to Flutter.
- Validate course, group, activity, and session authorization before issuing a LiveKit token.
- Use short-lived, room-scoped LiveKit tokens.
- Validate LiveKit webhook authenticity.
- Do not expose unnecessary Moodle data to mobile clients.
- Make synchronization idempotent.
- Store integration logs without credentials or token values.
- Return appropriate `401` and `403` responses.

## V1 compatibility

The current V1 may still contain local users, local roles, and local classes for development and testing. During migration:

- preserve existing live-session behavior;
- isolate local fallback behavior behind an explicit feature flag or adapter;
- do not silently mix local authorization with Moodle authorization;
- define how existing local users and classes are mapped or retired;
- provide a rollback strategy for the migration.

## Required tests

Add tests proving that:

- a valid Moodle identity can be mapped to a Micraa identity;
- invalid, expired, or incorrectly signed Moodle assertions are rejected;
- an authorized Moodle user can join the correct live session;
- a user not enrolled in the course receives `403 Forbidden`;
- a user without the required Moodle role receives `403 Forbidden`;
- LiveKit tokens are generated only after Moodle authorization succeeds;
- LiveKit tokens are scoped to the correct room and participant identity;
- repeated synchronization does not create duplicates;
- integration retries are safe;
- Moodle unavailability produces a controlled error;
- LiveKit webhooks are authenticated and idempotent;
- no secret is returned to Flutter;
- V1 local fallback behavior remains covered during migration.

## Deliverables

Provide:

1. the approved Moodle/Micraa responsibility matrix;
2. the chosen authentication and integration protocol with rationale;
3. the database schema and migration plan;
4. external identity and activity mapping services;
5. integration adapters and interfaces;
6. final REST endpoints and DTOs;
7. Spring Security changes;
8. Flutter changes;
9. synchronization and retry behavior;
10. tests and their results;
11. environment variables and secret-management requirements;
12. deployment and rollback instructions;
13. known limitations and decisions requiring product approval.

Do not claim Moodle V2 is complete until authentication, authorization, synchronization, error handling, LiveKit integration, and the required tests are implemented.
