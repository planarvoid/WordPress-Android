package com.soundcloud.android.onboarding.exceptions;

public class AddAccountException extends SignInException {
    public AddAccountException() {
        super("Unable to add account during sign in");
    }
}
