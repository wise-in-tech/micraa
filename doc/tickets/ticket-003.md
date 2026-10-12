# Ticket 003 - Google Sign-In (mobile)

## Flow

Mobile app + REST backend, so this is ID-token verification, not an OAuth2 redirect flow:

1. Flutter (`google_sign_in` v7) shows the native Google account picker and gets a Google **ID token**.
2. Flutter sends it to `POST /api/auth/google` `{"idToken": "..."}`.
3. The backend (`GoogleTokenVerifier`) checks the RS256 signature against Google's JWK set
   (`https://www.googleapis.com/oauth2/v3/certs`, cached), `exp`, `iss` = `accounts.google.com`,
   `aud` = one of `GOOGLE_AUTH_CLIENT_IDS`, and `email_verified = true`.
4. User resolution (`AuthService#loginWithGoogle`):
   - found by Google `sub` → log in;
   - else an account with the same verified email exists → link the Google `sub` to it (role kept);
     if that account is already linked to a *different* Google `sub` → 401;
   - else create a **STUDENT** with no password (same rule as public registration).
5. Response: the usual `AuthResponse` (our own JWT), identical to `/login` and `/register`.

Errors: missing `idToken` → 400; invalid/expired/foreign token, unverified email, or Google login
not configured → 401. A Google-only account can't use `/login` (no password).

## Setup

### Google Cloud Console (APIs & Services → Credentials)

Create OAuth client IDs in the same project:

| Type | Purpose |
|------|---------|
| **Web application** | `serverClientId` in Flutter → becomes the ID token `aud` on Android. Required. |
| **Android** | package `com.wiseintech.maqraa` + SHA-1 of each signing key (debug, release, Play App Signing). No ID to copy anywhere, but sign-in fails without it. |
| **iOS** (when the iOS app exists) | Bundle ID; its client ID is the `aud` on iOS. |

Debug SHA-1: `keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android`

### Backend

`.env`: `GOOGLE_AUTH_CLIENT_IDS=<web-client-id>[,<ios-client-id>]`. Empty = Google login disabled (fails closed).

### Database migration (existing PostgreSQL)

`ddl-auto=update` adds the new `google_subject` column but does **not** drop the `NOT NULL` on
`password_hash`, so creating a Google-only user would fail. Run once:

```sql
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;
```

### Flutter

- `lib/config/app_config.dart`: set `kGoogleServerClientId` to the **Web** client ID.
- iOS (not yet set up in this repo): add `GIDClientID` (iOS client ID) and `GIDServerClientID` (Web client ID)
  plus the reversed iOS client ID as a URL scheme in `ios/Runner/Info.plist`.
