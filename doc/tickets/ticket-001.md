# Ticket 001 - Immediate User, Class, and Mobile Live-Session Improvements

## Developer Prompt

You are a senior expert Java Spring Boot, Spring Security, PostgreSQL, and Flutter developer.

Work on the existing Micraa codebase. Read these documents first:

1. [Functional requirements](../FUNCTIONAL-REQUIREMENTS.md)
2. [Technical architecture](../skills/ARCHITECTURE.md)
3. [Development skill](../skills/school-live-classroom-development-skill.md)
4. [Docker operations guide](../operations/DOCKER.md)

This ticket contains the immediate code changes. Moodle integration is a separate future scope described in [ticket-002](ticket-002.md). Do not implement Moodle integration in this ticket.

## Scope

Implement the following changes while preserving the existing JWT, REST, Flutter, and LiveKit behavior:

1. Prevent public users from creating teacher accounts.
2. Allow only the currently supported administrative mechanism to create teacher accounts, without exposing that capability to public registration or ordinary users.
3. Allow teachers to see connected participants in their live class, ordered by connection time.
4. Keep the mobile live-room screen awake while the user is connected to a live class, so the phone does not lock and interrupt the session.
5. Allow a teacher to remotely mute or unmute a student's microphone in the live class.

## Account and role rules

The current application is still a V1 local implementation. It currently has `TEACHER` and `STUDENT`; do not add Moodle dependencies here.

### Public registration

`POST /api/auth/register` must always create a `STUDENT`.

- Ignore or reject any `role` field sent by the client.
- A request containing `role=TEACHER` must never create a teacher.
- A request containing `role=ADMIN` must never create an admin.
- Do not expose a role selector in Flutter registration.
- Keep the response compatible with the existing mobile application.

### Teacher account creation

Do not allow an unauthenticated user, student, or teacher to create a teacher account.

- Review the legacy `POST /api/users` endpoint.
- Remove, restrict, or replace any path that allows a public request to create a `TEACHER`.
- If an administrative endpoint is needed for the current V1, protect it with an explicit backend authorization rule and document the bootstrap mechanism.
- Never trust a role supplied in a request body as proof of authorization.
- Return `401` for unauthenticated callers and `403` for authenticated callers without permission.

Do not implement the complete Moodle-managed account model in this ticket. The detailed V2 ownership model is specified in `ticket-002.md`.

## Connected participants for teachers

When a teacher is inside a live class, the Flutter interface must show the currently connected participants.

The participant list must be ordered by connection time:

- earliest connection first, or use the opposite order only if the existing UX convention clearly requires it;
- document the selected order;
- use a stable timestamp or sequence value, not an unordered map iteration;
- update the list when a participant connects or disconnects;
- avoid exposing tokens, secrets, or unnecessary personal data.

The backend must provide or preserve the minimum information required by Flutter, such as:

- participant identity;
- display name;
- effective role;
- connection timestamp or monotonic connection sequence;
- current connection state.

LiveKit remains responsible for real-time participant transport. Spring Boot remains responsible for application authorization and persisted attendance data. Do not route audio through Spring Boot.

If the current LiveKit SDK already exposes a connection timestamp or event order, use it consistently. Otherwise introduce a small application-level sequence/timestamp mechanism and test it.

## Teacher microphone moderation

A teacher must be able to mute or unmute a student microphone during the active live class.

- Only the teacher assigned to the class may perform this action.
- A student must not be able to mute or unmute another participant.
- Enforce the action in the backend/LiveKit control path, not only by hiding a Flutter button.
- Use LiveKit's server-side participant or track control mechanism when a global mute is required.
- The teacher interface must show the current microphone state and the moderation action.
- The student interface must clearly show when the microphone was muted by the teacher.
- A student must not automatically re-enable a microphone that is server-muted.
- Preserve the distinction between a teacher-forced mute and a student's own local mute.

Suggested endpoints, subject to the existing API design:

```text
POST /api/live-classes/{id}/participants/{participantId}/mute
POST /api/live-classes/{id}/participants/{participantId}/unmute
```

The endpoint must verify the caller's JWT, class ownership, participant membership, and live-session status before issuing the LiveKit control command.

## Keep the mobile screen awake

While the user is connected to a live class, the Flutter live-room screen must prevent the device from locking.

- Use an established Flutter wakelock-compatible package or the project’s existing platform abstraction.
- Enable the wakelock when entering the live-room screen or when the LiveKit connection becomes active.
- Disable it when leaving the room, disconnecting, or when the widget is disposed.
- Handle connection failures and navigation away without leaving wakelock enabled globally.
- Do not prevent normal screen locking outside the live-room experience.
- Verify behavior on Android and keep the implementation compatible with iOS.

The live-room screen must also handle lifecycle transitions safely. A temporary background/foreground transition must not create duplicate listeners or leave stale wakelock state.

## Required implementation workflow

Before editing:

1. Inspect the current auth controller/service, user controller/service, security configuration, user model, and repositories.
2. Inspect the Flutter registration flow, live-room screen, LiveKit service, participant events, and `pubspec.yaml`.
3. Identify the smallest change that satisfies this ticket.
4. Confirm current API and UI behavior with focused tests or code inspection.

Implement incrementally. Do not rewrite unrelated code or implement Moodle in this ticket.

## Required backend tests

Add or update tests proving that:

- public registration always creates a `STUDENT`;
- a public request cannot create a `TEACHER` by sending `role=TEACHER`;
- an unauthenticated caller cannot create a teacher account;
- a student cannot create a teacher account;
- a teacher cannot create a teacher account;
- the authorized administrative path, if retained in V1, can create a teacher;
- unauthorized calls return the correct `401` or `403` status;
- connected participant data has a deterministic connection-time order;
- participant connect and disconnect events update the visible list correctly.
- only the assigned teacher can mute or unmute a student;
- a student cannot mute or unmute another participant;
- teacher mute/unmute commands update the participant microphone state;
- a forced mute cannot be bypassed by the student client.

## Required Flutter tests

Add or update tests proving that:

- registration does not show a role selector;
- the registration request cannot create a teacher;
- the teacher participant list is ordered by connection time;
- participant additions and removals update the list;
- wakelock is enabled while the live room is active;
- wakelock is disabled after disconnect, navigation away, and widget disposal;
- a failed connection does not leave wakelock enabled.
- the teacher can mute and unmute a student from the live-room UI;
- the student sees the forced-mute state and cannot bypass it.

## Acceptance criteria

- A normal user cannot create a teacher account from the API or Flutter application.
- Public registration always creates a student.
- Teacher participant lists are ordered consistently by connection time.
- The assigned teacher can remotely mute and unmute a student's microphone.
- Unauthorized users cannot control another participant's microphone.
- The phone screen remains awake during an active live-room session.
- The wakelock is released when the session ends.
- Existing login, class joining, audio, and LiveKit flows still work.
- No Moodle code or Moodle dependency is introduced by this ticket.
- Documentation is updated with the final endpoint behavior and test commands.

## Final report

Report:

1. Files changed.
2. Teacher-account creation behavior and authorization path.
3. Participant ordering implementation.
4. Flutter wakelock implementation.
5. Tests executed and results.
6. Any remaining limitations.

Do not claim a requirement is complete unless it is implemented in code and covered by a test.
