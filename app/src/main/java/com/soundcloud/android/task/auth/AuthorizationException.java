package com.soundcloud.android.task.auth;

import java.util.List;

/**
 * Custom exception to propogate pass API error messaging from Auth Tasks
 */
public class AuthorizationException extends Exception {

    private final String[] mErrors;

    public AuthorizationException(String... errors) {
        mErrors = errors;
    }

    public AuthorizationException(List<String> errors) {
        mErrors = errors.toArray(new String[errors.size()]);
    }

    public String[] getErrors() {
        return mErrors;
    }

    public String getFirstError() {
        return mErrors == null || mErrors.length == 0 ? "" : mErrors[0];
    }
}
