package com.soundcloud.android.events;

import com.soundcloud.java.strings.Strings;

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
    public static final int DEVICE_CONFLICT = 8;

    private static final String OPT_IN = "opt_in";
    private static final String ERROR_TYPE = "error_type";
    private static final String YES = "yes";
    private static final String NO = "no";
    private static final String DISMISS = "dismiss";
    private static final String SERVE_CAPTCHA = "serve_captcha";
    private static final String DENIED_SIGNUP = "denied_signup";
    private static final String EXISTING_EMAIL = "existing_email";
    private static final String INVALID_EMAIL = "invalid_email";
    private static final String FACEBOOK_EMAIL_DENIED = "facebook_email_denied";
    private static final String GENERAL_ERROR = "general_error";
    private static final String DEVICE_LIMIT = "device_limit";
    private static final String LOGGED_OUT = "logged_out";
    private static final String TYPE = "type";
    private static final String ACTION = "action";
    private static final String SIGN_UP = "sign up";
    private static final String LOG_IN = "sign in";
    private static final String NATIVE = "native";
    private static final String GOOGLE_PLUS = "google_plus";
    private static final String FACEBOOK = "facebook";
    private static final String ACCEPT = "accept";
    private static final String CANCEL = "cancel";
    private static final String USER_INFO_ADDED_USERNAME = "added_username";
    private static final String USER_INFO_ADDED_PICTURE = "added_picture";

    private final int kind;
    private final Map<String, String> attributes;

    private OnboardingEvent(int kind) {
        this.kind = kind;
        attributes = new HashMap<>();
    }

    public static OnboardingEvent signUpPrompt() {
        return new OnboardingEvent(AUTH_PROMPT).put(TYPE, SIGN_UP);
    }

    public static OnboardingEvent logInPrompt() {
        return new OnboardingEvent(AUTH_PROMPT).put(TYPE, LOG_IN);
    }

    public static OnboardingEvent nativeAuthEvent() {
        return new OnboardingEvent(AUTH_CREDENTIALS).put(TYPE, NATIVE);
    }

    public static OnboardingEvent googleAuthEvent() {
        return new OnboardingEvent(AUTH_CREDENTIALS).put(TYPE, GOOGLE_PLUS);
    }

    public static OnboardingEvent facebookAuthEvent() {
        return new OnboardingEvent(AUTH_CREDENTIALS).put(TYPE, FACEBOOK);
    }

    public static OnboardingEvent termsAccepted() {
        return new OnboardingEvent(CONFIRM_TERMS).put(ACTION, ACCEPT);
    }

    public static OnboardingEvent termsRejected() {
        return new OnboardingEvent(CONFIRM_TERMS).put(ACTION, CANCEL);
    }

    public static OnboardingEvent authComplete() {
        return new OnboardingEvent(AUTH_COMPLETE);
    }

    @SuppressWarnings("PMD.ConfusingTernary")
    public static OnboardingEvent savedUserInfo(String username, File avatarFile) {
        return new OnboardingEvent(USER_INFO)
                .put(USER_INFO_ADDED_USERNAME, Strings.isNotBlank(username) ? YES : NO)
                .put(USER_INFO_ADDED_PICTURE, avatarFile != null ? YES : NO);
    }

    public static OnboardingEvent skippedUserInfo() {
        return new OnboardingEvent(USER_INFO)
                .put(USER_INFO_ADDED_USERNAME, NO)
                .put(USER_INFO_ADDED_PICTURE, NO);
    }

    public static OnboardingEvent onboardingComplete() {
        return new OnboardingEvent(ONBOARDING_COMPLETE);
    }

    public static OnboardingEvent acceptEmailOptIn() {
        return new OnboardingEvent(EMAIL_MARKETING).put(OPT_IN, YES);
    }

    public static OnboardingEvent rejectEmailOptIn() {
        return new OnboardingEvent(EMAIL_MARKETING).put(OPT_IN, NO);
    }

    public static OnboardingEvent dismissEmailOptIn() {
        return new OnboardingEvent(EMAIL_MARKETING).put(OPT_IN, DISMISS);
    }

    public static OnboardingEvent signupServeCaptcha() {
        return new OnboardingEvent(SIGNUP_ERROR).put(ERROR_TYPE, SERVE_CAPTCHA);
    }

    public static OnboardingEvent signupDenied() {
        return new OnboardingEvent(SIGNUP_ERROR).put(ERROR_TYPE, DENIED_SIGNUP);
    }

    public static OnboardingEvent signupExistingEmail() {
        return new OnboardingEvent(SIGNUP_ERROR).put(ERROR_TYPE, EXISTING_EMAIL);
    }

    public static OnboardingEvent signupInvalidEmail() {
        return new OnboardingEvent(SIGNUP_ERROR).put(ERROR_TYPE, INVALID_EMAIL);
    }

    public static OnboardingEvent signupFacebookEmailDenied() {
        return new OnboardingEvent(SIGNUP_ERROR).put(ERROR_TYPE, FACEBOOK_EMAIL_DENIED);
    }

    public static OnboardingEvent signupGeneralError() {
        return new OnboardingEvent(SIGNUP_ERROR).put(ERROR_TYPE, GENERAL_ERROR);
    }

    public static OnboardingEvent deviceConflictOnLogin() {
        return new OnboardingEvent(DEVICE_CONFLICT).put(ERROR_TYPE, DEVICE_LIMIT);
    }

    public static OnboardingEvent deviceConflictLoggedOut() {
        return new OnboardingEvent(DEVICE_CONFLICT).put(ERROR_TYPE, LOGGED_OUT);
    }

    public int getKind() {
        return kind;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    private OnboardingEvent put(String key, String value) {
        attributes.put(key, value);
        return this;
    }

    @Override
    public String toString() {
        return String.format("Onboarding Event with type '%s' and %s", kindToString(kind), attributes.toString());
    }

    private static String kindToString(int kind) {
        switch (kind) {
            case AUTH_PROMPT:
                return "auth prompt";
            case AUTH_CREDENTIALS:
                return "auth credentials";
            case CONFIRM_TERMS:
                return "confirm terms";
            case AUTH_COMPLETE:
                return "auth complete";
            case USER_INFO:
                return "user info";
            case ONBOARDING_COMPLETE:
                return "onboarding complete";
            case EMAIL_MARKETING:
                return "email marketing";
            case SIGNUP_ERROR:
                return "signup error";
            case DEVICE_CONFLICT:
                return "device conflict";
            default:
                return "unknown " + kind;
        }
    }

}
