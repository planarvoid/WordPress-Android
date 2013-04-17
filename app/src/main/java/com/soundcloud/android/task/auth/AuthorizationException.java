package com.soundcloud.android.task.auth;

/**
 * Temporary we can propogate error messages better. this should rarely happen
 */

@Deprecated
public class AuthorizationException extends Exception {

    private final int mErrorMessage;

    public AuthorizationException(int errorMessage) {
        mErrorMessage = errorMessage;
    }
}
