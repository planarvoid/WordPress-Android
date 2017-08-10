package com.soundcloud.android.onboarding.auth.tasks;

/**
 * Custom exception to propagate pass API error messaging from Auth Tasks
 */
public class AuthTaskException extends Exception {

    private final String[] errors;

    public AuthTaskException(String... errors) {
        this.errors = errors;
    }

    public String getFirstError() {
        return errors == null || errors.length == 0 ? "" : errors[0];
    }
}
