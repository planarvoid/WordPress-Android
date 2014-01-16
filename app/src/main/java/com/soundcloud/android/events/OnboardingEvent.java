package com.soundcloud.android.events;


import com.soundcloud.android.utils.ScTextUtils;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class OnboardingEvent implements Event {

    public static final int AUTH_PROMPT = 0;
    public static final int AUTH_CREDENTIALS = 1;
    public static final int CONFIRM_TERMS = 2;
    public static final int AUTH_COMPLETE = 3;
    public static final int SAVE_USER_INFO = 4;
    public static final int SKIP_USER_INFO = 5;
    public static final int ONBOARDING_COMPLETE = 6;

    private final int mKind;
    private final Map<String, String> mAttributes;

    private OnboardingEvent(int kind, Map<String, String> attributes) {
        mKind = kind;
        mAttributes = attributes;
    }

    private OnboardingEvent(int type) {
        this(type, Collections.<String, String>emptyMap());
    }

    public static OnboardingEvent signUpPrompt() {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("type", "sign up");
        return new OnboardingEvent(AUTH_PROMPT, attributes);
    }

    public static OnboardingEvent logInPrompt() {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("type", "log in");
        return new OnboardingEvent(AUTH_PROMPT, attributes);
    }

    public static OnboardingEvent nativeAuthEvent(){
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("type", "native");
        return new OnboardingEvent(AUTH_CREDENTIALS, attributes);
    }

    public static OnboardingEvent googleAuthEvent(){
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("type", "google_plus");
        return new OnboardingEvent(AUTH_CREDENTIALS, attributes);
    }

    public static OnboardingEvent facebookAuthEvent(){
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("type", "facebook");
        return new OnboardingEvent(AUTH_CREDENTIALS, attributes);
    }

    public static OnboardingEvent termsAccepted(){
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("action", "accept");
        return new OnboardingEvent(CONFIRM_TERMS, attributes);
    }

    public static OnboardingEvent termsRejected(){
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("action", "cancel");
        return new OnboardingEvent(CONFIRM_TERMS, attributes);
    }

    public static OnboardingEvent authComplete(){
        return new OnboardingEvent(AUTH_COMPLETE);
    }

    public static OnboardingEvent savedUserInfo(String username, File avatarFile){
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("added_username", ScTextUtils.isNotBlank(username) ? "yes" : "no");
        attributes.put("added_picture", avatarFile != null ? "yes" : "no");
        return new OnboardingEvent(SAVE_USER_INFO, attributes);
    }

    public static OnboardingEvent skippedUserInfo(){
        return new OnboardingEvent(SKIP_USER_INFO);
    }

    public static OnboardingEvent onboardingComplete(){
        return new OnboardingEvent(ONBOARDING_COMPLETE);
    }

    @Override
    public int getKind() {
        return mKind;
    }

    public Map<String, String> getAttributes() {
        return mAttributes;
    }

    @Override
    public String toString() {
        return  String.format("onboarding Event with type id %s and %s", mKind, mAttributes.toString());
    }
}
