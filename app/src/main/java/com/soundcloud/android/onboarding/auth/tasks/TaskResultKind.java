package com.soundcloud.android.onboarding.auth.tasks;

import java.util.EnumSet;

public enum TaskResultKind {
    SUCCESS,
    FAILURE,
    EMAIL_TAKEN,
    SPAM,
    DENIED,
    EMAIL_INVALID,
    FLAKY_SIGNUP_ERROR,
    DEVICE_CONFLICT,
    DEVICE_BLOCK,
    UNAUTHORIZED,
    NETWORK_ERROR,
    SERVER_ERROR,
    VALIDATION_ERROR;

    private static final EnumSet<TaskResultKind> UNEXPECTED_ERRORS = EnumSet.of(FAILURE,
                                                                                FLAKY_SIGNUP_ERROR,
                                                                                SERVER_ERROR,
                                                                                VALIDATION_ERROR,
                                                                                NETWORK_ERROR,
                                                                                SPAM,
                                                                                DENIED,
                                                                                UNAUTHORIZED);

    public boolean isUnexpectedError() {
        return UNEXPECTED_ERRORS.contains(this);
    }

}
