package com.wiseintech.micraa.security;

/**
 * Claims extracted from a verified Google ID token.
 *
 * @param subject Google's stable account ID ("sub") - never changes, unlike the email
 * @param email   verified email address
 * @param name    display name, may be null
 */
public record GoogleIdentity(String subject, String email, String name) {
}
