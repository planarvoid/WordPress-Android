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
    public static final int EMAIL_MARKETING = 6;
    public static final int SIGNUP_ERROR = 7;

    private final int kind;
    private final Map<String, String> attributes;

    private OnboardingEvent(int kind) {
        this.kind = kind;
        attributes = new HashMap<String, String>();
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

    public static OnboardingEvent acceptEmailOptIn() {
        return new OnboardingEvent(EMAIL_MARKETING).put("opt_in", "yes");
    }

    public static OnboardingEvent rejectEmailOptIn() {
        return new OnboardingEvent(EMAIL_MARKETING).put("opt_in", "no");
    }

    public static OnboardingEvent dismissEmailOptIn() {
        return new OnboardingEvent(EMAIL_MARKETING).put("opt_in", "dismiss");
    }

    public static OnboardingEvent signupServeCaptcha() {
        return new OnboardingEvent(SIGNUP_ERROR).put("error_type", "serve_captcha");
    }

    public static OnboardingEvent signupDenied() {
        return new OnboardingEvent(SIGNUP_ERROR).put("error_type", "denied_signup");
    }

    public static OnboardingEvent signupExistingEmail() {
        return new OnboardingEvent(SIGNUP_ERROR).put("error_type", "existing_email");
    }

    public static OnboardingEvent signupInvalidEmail() {
        return new OnboardingEvent(SIGNUP_ERROR).put("error_type", "invalid_email");
    }

    public static OnboardingEvent signupGeneralError() {
        return new OnboardingEvent(SIGNUP_ERROR).put("error_type", "general_error");
    }

    public int getKind() {
        return kind;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    @Override
    public String toString() {
        return String.format("Onboarding Event with type id %s and %s", kind, attributes.toString());
    }

    private OnboardingEvent put(String key, String value) {
        attributes.put(key, value);
        return this;
    }
}
