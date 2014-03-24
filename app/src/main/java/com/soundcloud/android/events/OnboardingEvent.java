package com.soundcloud.android.events;

import com.soundcloud.android.utils.ScTextUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public final class OnboardingEvent {

    public static final int AUTH_PROMPT = 0;
    public static final int AUTH_CREDENTIALS = 1;
    public static final int CONFIRM_TERMS = 2;
    public static final int AUTH_COMPLETE = 3;
    public static final int USER_INFO = 4;
    public static final int ONBOARDING_COMPLETE = 5;

    private final int mKind;
    private final Map<String, String> mAttributes;

    private OnboardingEvent(int kind) {
        mKind = kind;
        mAttributes = new HashMap<String, String>();
    }

    public static OnboardingEvent signUpPrompt() {
        return new OnboardingEvent(AUTH_PROMPT).put("type", "sign up");
    }

    public static OnboardingEvent logInPrompt() {
        return new OnboardingEvent(AUTH_PROMPT).put("type", "log in");

    }

    public static OnboardingEvent nativeAuthEvent() {
        return new OnboardingEvent(AUTH_CREDENTIALS).put("type", "native");
    }

    public static OnboardingEvent googleAuthEvent() {
        return new OnboardingEvent(AUTH_CREDENTIALS).put("type", "google_plus");
    }

    public static OnboardingEvent facebookAuthEvent() {
        return new OnboardingEvent(AUTH_CREDENTIALS).put("type", "facebook");
    }

    public static OnboardingEvent termsAccepted() {
        return new OnboardingEvent(CONFIRM_TERMS).put("action", "accept");
    }

    public static OnboardingEvent termsRejected() {
        return new OnboardingEvent(CONFIRM_TERMS).put("action", "cancel");
    }

    public static OnboardingEvent authComplete() {
        return new OnboardingEvent(AUTH_COMPLETE);
    }

    public static OnboardingEvent savedUserInfo(String username, File avatarFile) {
        return new OnboardingEvent(USER_INFO)
                .put("added_username", ScTextUtils.isNotBlank(username) ? "yes" : "no")
                .put("added_picture", avatarFile != null ? "yes" : "no");
    }

    public static OnboardingEvent skippedUserInfo() {
        return new OnboardingEvent(USER_INFO)
                .put("added_username", "no")
                .put("added_picture", "no");
    }

    public static OnboardingEvent onboardingComplete() {
        return new OnboardingEvent(ONBOARDING_COMPLETE);
    }

    public int getKind() {
        return mKind;
    }

    public Map<String, String> getAttributes() {
        return mAttributes;
    }

    @Override
    public String toString() {
        return String.format("Onboarding Event with type id %s and %s", mKind, mAttributes.toString());
    }

    private OnboardingEvent put(String key, String value) {
        mAttributes.put(key, value);
        return this;
    }
}
