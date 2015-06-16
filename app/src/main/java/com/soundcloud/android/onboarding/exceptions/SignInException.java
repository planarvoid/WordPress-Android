package com.soundcloud.android.onboarding.exceptions;

public class SignInException extends RuntimeException {
    public SignInException(String message) {
        super(message);
    }

    public SignInException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
