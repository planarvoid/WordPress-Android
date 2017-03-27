package com.soundcloud.android.onboarding;

public enum LoginProvider {
    PASSWORD("password"),
    FACEBOOK("facebook"),
    GOOGLE("google");

    private final String provider;

    LoginProvider(String provider) {
        this.provider = provider;
    }

    @Override
    public String toString() {
        return provider;
    }
}
