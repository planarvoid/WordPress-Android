package com.soundcloud.android.onboarding.exceptions;

public class TokenRetrievalException extends SignInException {
    public TokenRetrievalException(Throwable t) {
        super("error retrieving SC API token", t);
    }
}
