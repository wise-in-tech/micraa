# School Live Classroom Platform — Development Skill

## Purpose

Build the first version of a school e-learning platform focused on one core capability:

> A teacher can open a live classroom, and authorized students can join and communicate with the teacher and each other through high-quality, low-latency two-way audio and text chat.

The first versifson must remain simple. Do not build a complete LMS at this stage.

---

## Product Goal

The platform is intended for a school and will eventually become a broader e-learning platform.

For the first version, the priority is to create a reliable and pleasant live classroom experience.

The most important requirement is:

**Excellent real-time audio quality and reliability.**

Teacher and students must be able to:

- Hear each other clearly
- Speak to each other in real time
- Mute/unmute microphones
- See who is currently connected
- Exchange text messages
- Join and leave the classroom reliably

The first version does not require video.

---

## User Roles

There are initially two roles:

### Teacher

A teacher can:

- Log in
- See their live classes
- Start a live class
- Join their live class
- See connected participants
- Communicate using audio
- Mute/unmute their microphone
- Use text chat
- End the live class

### Student

A student can:

- Log in
- See classes they are allowed to attend
- Join an active live class
- Hear the teacher and other participants
- Speak using their microphone
- Mute/unmute their microphone
- See connected participants
- Use text chat
- Leave the classroom

---

# Core Architecture

The initial architecture should be:

```text
                       Mobile Application
                       Teacher / Student
                              |
                              |
                         REST / HTTPS
                              |
                              v
                    +--------------------+
                    |    Spring Boot     |
                    |                    |
                    | Authentication     |
                    | Authorization      |
                    | Users              |
                    | Live Classes       |
                    | Class Membership   |
                    | LiveKit Tokens     |
                    +---------+----------+
                              |
                              |
                         PostgreSQL
                              |
                              |
                       LiveKit Webhooks
                              ^
                              |
                              |
                    +---------+----------+
                    |       LiveKit      |
                    |                    |
                    | WebRTC             |
                    | Real-time Audio    |
                    | Participants       |
                    | Chat               |
                    +---------+----------+
                              ^
                              |
                     Direct real-time
                       media connection
                              |
                    +---------+----------+
                    |                    |
                 Teacher             Students
```

---

## Important Separation of Responsibilities

### Spring Boot

Spring Boot is responsible for the application's business logic:

- Authentication
- Authorization
- Users
- Teachers
- Students
- Live classes
- Class membership
- Starting/ending classes
- Generating LiveKit access tokens
- Persisting attendance and class state
- Processing LiveKit webhooks

Spring Boot must NOT be used to transport the live audio.

### LiveKit

LiveKit is responsible for real-time communication:

- WebRTC
- Real-time audio
- Participant connections
- Audio streams
- Connection/reconnection handling
- Participant presence
- Real-time chat/data communication

The mobile application connects directly to LiveKit for the real-time media connection.

The LiveKit API secret must NEVER be exposed to the mobile application.

---

## WebRTC Concept

The application does not need to implement WebRTC itself.

The important communication flow is:

```text
Teacher microphone
       |
       v
     LiveKit
       |
       +----> Student
       |
       +----> Student
       |
       +----> Student
```

And in the other direction:

```text
Student microphone
       |
       v
     LiveKit
       |
       +----> Teacher
       |
       +----> Other students
```

Spring Boot is not in the audio path.

---

# LiveKit Token Flow

When a user wants to join a classroom:

```text
Mobile App
    |
    | POST /api/live-classes/{id}/join
    |
    v
Spring Boot
    |
    | Check authentication
    | Check authorization
    | Check class status
    | Check student membership
    |
    | Generate LiveKit token
    |
    v
Mobile App
    |
    | LiveKit URL + token
    |
    v
LiveKit
```

The backend must control who is allowed to join each classroom.

Never rely only on the LiveKit room name as a security mechanism.

---

# LiveKit Webhooks

LiveKit should send webhook events to the Spring Boot backend.

For example:

```text
Student joins
     |
     v
LiveKit
     |
     | webhook
     v
Spring Boot
     |
     v
PostgreSQL
```

Use these events to maintain application state.

Examples:

- Participant joined
- Participant left
- Room started
- Room ended
- Participant published/unpublished audio

This information can be used to:

- Track attendance
- Record join/leave times
- Calculate attendance duration
- Maintain the current participant list
- Detect when a classroom has ended
- Trigger future notifications

Webhooks are for application events/state.

They are NOT used to transport the audio.

---

# Initial Data Model

Keep the data model minimal.

## User

```text
id
name
email
passwordHash
role
createdAt
```

Roles:

```text
TEACHER
STUDENT
```

## LiveClass

```text
id
title
teacherId
status
scheduledAt
startedAt
endedAt
createdAt
```

Possible statuses:

```text
SCHEDULED
LIVE
ENDED
```

## LiveClassStudent

```text
liveClassId
studentId
```

## Attendance

Use a simple attendance model to record:

```text
id
liveClassId
studentId
joinedAt
leftAt
```

Do not create unnecessary entities until they are required.

---

# Initial REST API

The exact API design can be refined during implementation, but the platform should support operations similar to:

```text
POST   /api/auth/login

GET    /api/live-classes
POST   /api/live-classes
GET    /api/live-classes/{id}

POST   /api/live-classes/{id}/start
POST   /api/live-classes/{id}/join
POST   /api/live-classes/{id}/end

POST   /api/livekit/webhook
```

The `join` endpoint should:

1. Authenticate the user.
2. Verify that the user is allowed to join the class.
3. Verify that the class is active.
4. Generate an appropriate short-lived LiveKit token.
5. Return the LiveKit connection information.

---

# Security

Use proper authentication and authorization.

Requirements:

- Users must authenticate.
- Students can only join classes they are authorized to attend.
- Only the teacher responsible for a class can start/end it.
- LiveKit API credentials remain on the backend.
- LiveKit tokens should be short-lived.
- Never trust user IDs or roles supplied by the mobile client.
- Validate all API requests.
- Validate LiveKit webhook authenticity according to LiveKit's security mechanism.

---

# Authentication & Authorization — Deep Dive

This section explains the concepts of OIDC, JWT, role management, and the two-token architecture used in this platform.

---

## Concept: Two Types of Tokens

This platform uses **two completely different tokens** that serve different purposes:

```text
┌────────────────────────────────────────────────────────────────┐
│  TOKEN 1 — Application JWT (Auth Token)                        │
│                                                                │
│  Issued by:   Spring Boot (our backend)                        │
│  Purpose:     Prove who you are to Spring Boot                 │
│  Lifetime:    Long  (e.g. 7 days)                              │
│  Used on:     Every REST API call                              │
│  Contains:    userId, email, role (TEACHER / STUDENT)          │
└────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────┐
│  TOKEN 2 — LiveKit Access Token                                │
│                                                                │
│  Issued by:   Spring Boot (using the LiveKit API secret)       │
│  Purpose:     Prove you are allowed to join one specific room  │
│  Lifetime:    Short (e.g. 10 minutes)                          │
│  Used on:     Direct connection to LiveKit server ONLY         │
│  Contains:    identity, roomName, permissions (canPublish etc) │
└────────────────────────────────────────────────────────────────┘
```

The Flutter app always needs **Token 1** first.
Token 2 is only requested when the user wants to join a live classroom.

---

## Concept: JWT (JSON Web Token)

A JWT is a compact, self-contained token.

It has three parts separated by dots:

```text
header.payload.signature
```

Example decoded payload (Token 1 — our auth token):

```json
{
  "sub": "42",
  "email": "martin@school.fr",
  "role": "TEACHER",
  "iss": "micraa-backend",
  "iat": 1724832000,
  "exp": 1725436800
}
```

Key fields:

| Field | Meaning |
|-------|---------|
| `sub` | Subject — the user's ID in our database |
| `email` | The user's email |
| `role` | `TEACHER` or `STUDENT` |
| `iss` | Issuer — identifies our backend |
| `iat` | Issued At — Unix timestamp |
| `exp` | Expiry — Unix timestamp |

The **signature** is computed using a secret key known only to the backend.

This means:
- The Flutter app **can read** the payload (it is base64 encoded, not encrypted)
- The Flutter app **cannot forge** a token (it does not know the secret key)
- The backend **verifies** the signature on every request

---

## Concept: OIDC vs Custom JWT — Which to Use?

### What is OIDC?

OpenID Connect (OIDC) is a standard authentication protocol built on top of OAuth 2.0.

It is used when you delegate authentication to an **external Identity Provider (IdP)**:

```text
Examples of OIDC Identity Providers:
  - Google (Sign in with Google)
  - Microsoft Entra ID (Azure AD)
  - Keycloak (self-hosted)
  - Auth0
  - Okta
```

Flow with an external IdP:

```text
User clicks "Login with Google"
        |
        v
Redirected to Google login page
        |
        v
Google authenticates the user
        |
        v
Google returns an ID Token (JWT) to our backend
        |
        v
Our backend validates the token, finds or creates the user
        |
        v
Our backend issues its own application JWT
        |
        v
Flutter app stores and uses our JWT
```

### When to Use OIDC

Use OIDC when:
- The school already has a Microsoft/Google identity system
- You want "Sign in with Google / Microsoft"
- You want to avoid managing passwords entirely
- You need to integrate with an existing school directory (e.g. Active Directory)

### When to Use Custom JWT (our current approach)

Use a custom JWT when:
- You want full control over users and passwords
- The school does not have an external IdP
- You want simplicity for V1
- You manage registration and login yourself

### Decision for This Platform

**V1: Custom JWT with Spring Security.**

This is simpler, has no external dependencies, and gives full control.

OIDC with an external IdP (e.g. Keycloak or Google) can be added later as an alternative login method without changing the rest of the architecture.

---

## Authentication Flow (V1 — Custom JWT)

### Registration

```text
Flutter App
    |
    | POST /api/auth/register
    | { name, email, password, role }
    |
    v
Spring Boot
    |
    | Validate input
    | Check email not already taken
    | BCrypt hash the password
    | Save User to PostgreSQL
    | Generate JWT
    |
    v
Flutter App
    |
    | Store JWT securely (flutter_secure_storage)
    | Navigate to home screen
```

### Login

```text
Flutter App
    |
    | POST /api/auth/login
    | { email, password }
    |
    v
Spring Boot
    |
    | Find user by email
    | BCrypt.verify(password, storedHash)
    | If valid → generate JWT
    | If invalid → 401 Unauthorized
    |
    v
Flutter App
    |
    | Store JWT (flutter_secure_storage)
    | Decode payload to read role
    | Navigate to Teacher screen OR Student screen
```

### Every Subsequent API Call

```text
Flutter App
    |
    | GET /api/live-classes
    | Header: Authorization: Bearer <JWT>
    |
    v
Spring Boot — JwtAuthFilter
    |
    | Extract token from header
    | Verify signature
    | Check expiry
    | Load user from token claims
    | Set SecurityContext (user is now "authenticated")
    |
    v
Spring Boot — Controller / Service
    |
    | @PreAuthorize("hasRole('TEACHER')") — role check
    | Business logic
    |
    v
Flutter App
    |
    | 200 OK + data
```

---

## Role Management

### Roles in This Platform

```text
TEACHER
  - Can create live classes
  - Can start their own classes
  - Can end their own classes
  - Can see all students
  - Cannot join classes as a student

STUDENT
  - Can see classes they are enrolled in
  - Can join LIVE classes they are enrolled in
  - Cannot start or end classes
  - Cannot create classes
```

### How Roles Are Enforced

Roles are enforced at **two levels**:

#### Level 1 — Spring Security (endpoint level)

```java
// Only TEACHER can start a class
@PostMapping("/{id}/start")
@PreAuthorize("hasRole('TEACHER')")
public ResponseEntity<?> startClass(...) { }

// Only STUDENT can join as student
@PostMapping("/{id}/join")
@PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
public ResponseEntity<?> joinClass(...) { }
```

#### Level 2 — Business logic (ownership check)

```java
// Inside the service — even if the role is TEACHER,
// only the teacher who OWNS the class can start it
public void startClass(Long classId, Long requestingUserId) {
    LiveClass liveClass = repository.findById(classId)...;

    if (!liveClass.getTeacherId().equals(requestingUserId)) {
        throw new AccessDeniedException("You are not the teacher of this class");
    }
    // proceed
}
```

#### Level 3 — LiveKit token permissions

```java
// Teacher gets publish + subscribe
AccessToken teacherToken = new AccessToken(apiKey, apiSecret);
teacherToken.addGrant(new RoomJoin(true));   // can join
teacherToken.addGrant(new CanPublish(true)); // can speak
teacherToken.addGrant(new CanSubscribe(true)); // can hear others

// Student also gets publish (bidirectional audio is the goal)
// A future "view-only" mode could set CanPublish to false
AccessToken studentToken = new AccessToken(apiKey, apiSecret);
studentToken.addGrant(new RoomJoin(true));
studentToken.addGrant(new CanPublish(true));
studentToken.addGrant(new CanSubscribe(true));
```

---

## The LiveKit Token Flow in Detail

This is the most critical flow to understand:

```text
Step 1 — Flutter has Auth JWT from login
         Authorization: Bearer eyJ...

Step 2 — Teacher clicks "Start Class"
         POST /api/live-classes/5/start
         Spring Boot:
           - Verifies JWT → extracts userId=1, role=TEACHER
           - Checks class 5 belongs to teacher 1
           - Sets class status = LIVE

Step 3 — Teacher clicks "Join Class"
         POST /api/live-classes/5/join
         Spring Boot:
           - Verifies JWT → userId=1, role=TEACHER
           - Verifies class 5 is LIVE
           - Generates LiveKit token:
               identity  = "teacher-1"
               room      = "class-5"
               canPublish = true
               canSubscribe = true
               expiry    = now + 10 minutes
           - Signs with LiveKit API secret (NEVER sent to client)
           - Returns:
               { livekitUrl, accessToken, roomName }

Step 4 — Flutter connects directly to LiveKit
         livekit.connect(livekitUrl, accessToken)
         LiveKit validates the token signature
         Teacher is now in the room

Step 5 — Student clicks "Join Class"
         POST /api/live-classes/5/join
         Spring Boot:
           - Verifies JWT → userId=2, role=STUDENT
           - Checks student 2 is enrolled in class 5
           - Checks class 5 is LIVE
           - Generates LiveKit token:
               identity  = "student-2"
               room      = "class-5"
               expiry    = now + 10 minutes
           - Returns: { livekitUrl, accessToken, roomName }

Step 6 — Flutter (student) connects to LiveKit
         Student is now in the same room
         Audio flows directly Teacher ↔ LiveKit ↔ Student
```

**Key security principles in this flow:**

- The backend validates the auth JWT before generating any LiveKit token
- The LiveKit API secret never leaves the backend
- The LiveKit token is bound to a specific room and identity
- Even if a student intercepts a LiveKit token, they can only join one specific room once, for 10 minutes
- The student cannot join a class they are not enrolled in — enforced before token generation

---

## Token Storage in Flutter

Never store tokens in plain `SharedPreferences` on mobile.

Use `flutter_secure_storage` which uses the OS keychain:

```dart
// pubspec.yaml
// flutter_secure_storage: ^9.0.0

class TokenStorage {
  static const _storage = FlutterSecureStorage();
  static const _key = 'auth_token';

  static Future<void> save(String token) =>
      _storage.write(key: _key, value: token);

  static Future<String?> read() =>
      _storage.read(key: _key);

  static Future<void> delete() =>
      _storage.delete(key: _key);
}
```

---

## Token Expiry and Refresh Strategy

### V1 — Simple approach

- Auth JWT validity: **7 days**
- On 401 response: redirect to login screen
- No refresh token in V1

### Future — Refresh Token pattern

```text
Auth JWT         → short lived (15 minutes)
Refresh Token    → long lived (30 days), stored securely
                   used only to get a new Auth JWT
                   rotated on every use
```

This pattern eliminates the need to re-enter credentials frequently while limiting the damage if an Auth JWT is stolen.

---

## Security Rules Summary

| Rule | Why |
|------|-----|
| Passwords hashed with BCrypt (cost ≥ 12) | One-way hash, brute-force resistant |
| JWT signed with HS256 + strong secret (≥ 256 bits) | Prevents token forgery |
| JWT validated on every request | Stateless, no server-side session |
| Role checked at endpoint level | Prevents wrong-role access |
| Ownership checked at service level | Prevents cross-user access |
| LiveKit API secret server-side only | Cannot be extracted from the app |
| LiveKit token short-lived (10 min) | Limits damage if intercepted |
| HTTPS everywhere in production | Prevents token interception |
| `flutter_secure_storage` for JWT | OS-level keychain protection |
| Never trust `userId` or `role` from request body | Always read from validated JWT |

---

# Technology

Preferred technology:

### Mobile

Flutter

### Backend

Java + Spring Boot

Use idiomatic Spring Boot architecture.

Recommended:

- Spring Security
- Spring Data JPA
- REST APIs
- PostgreSQL

### Real-time Communication

LiveKit

### Deployment

Docker

The initial backend should be a **modular monolith**.

Do not introduce microservices unless there is a concrete requirement.

---

# Live Classroom — V1

The classroom UI should remain simple.

It should provide:

```text
Teacher
Students
Microphone status
Mute/unmute
Participant list
Text chat
Leave
```

No video initially.

No screen sharing initially.

No recording initially.

No AI functionality initially.

---

# Explicitly Out of Scope for V1

Do NOT implement:

- Full LMS
- Moodle integration
- Course management
- Lessons
- Assignments
- Exams
- Gradebook
- Video
- Screen sharing
- Recording
- AI tutor
- AI grading
- AI summaries
- Advanced analytics
- Parent accounts
- Complex scheduling
- Microservices

These may be added later.

---

# Future Direction

The platform may eventually evolve into a complete school e-learning platform.

Possible future capabilities:

- Courses
- Lessons
- Assignments
- Exams
- Grades
- Student progress
- Recordings
- Video
- Screen sharing
- Speech-to-text
- AI-generated class summaries
- AI tutor
- Personalized learning
- AI-generated exercises
- Learning analytics
- Moodle integration if useful

The current architecture should allow these capabilities to be added without unnecessarily complicating V1.

---

# Development Principles

Prioritize:

1. Audio quality
2. Reliability
3. Security
4. Simple user experience
5. Simple architecture
6. Extensibility

Do not over-engineer the first version.

Do not implement WebRTC yourself.

Do not put real-time audio traffic through Spring Boot.

Keep LiveKit-specific logic isolated behind appropriate backend services.

Use clear domain terminology such as:

```text
User
Teacher
Student
LiveClass
Attendance
```

---

# Development Process

Before implementing substantial code:

1. Review these requirements.
2. Propose the final project structure.
3. Identify any important technical decisions.
4. Define the database schema.
5. Define the REST API.
6. Define the LiveKit integration and token flow.
7. Define the webhook handling.
8. Define the authentication/authorization flow.
9. Implement incrementally.
10. Add tests for the important business rules.

Do not generate the entire application in one step.

Build and validate the live classroom flow first.

The first successful milestone should be:

> **One teacher and multiple students can join the same live classroom, speak to each other with good-quality two-way audio, see the participants, and exchange text messages.**
