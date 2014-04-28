package com.soundcloud.android.onboarding.auth.tasks;

import java.util.List;

/**
 * Custom exception to propogate pass API error messaging from Auth Tasks
 */
public class AuthTaskException extends Exception {

    private final String[] errors;

    public AuthTaskException(String... errors) {
        this.errors = errors;
    }

    public AuthTaskException(List<String> errors) {
        this.errors = errors.toArray(new String[errors.size()]);
    }

    public String[] getErrors() {
        return errors;
    }

    public String getFirstError() {
        return errors == null || errors.length == 0 ? "" : errors[0];
    }
}
