package com.soundcloud.android.activity.auth;

import android.content.Intent;

public enum SignupVia {
    API("api"),
    FACEBOOK_SSO("facebook:access-token"),
    FACEBOOK_WEBFLOW("facebook:web-flow"),
    UNKNOWN("unknown");

    public static final String EXTRA = "signed_up";

    public final String name;

    private SignupVia(String name) {
        this.name = name;
    }

    public static SignupVia fromIntent(Intent intent) {
        return fromString(intent.getStringExtra(EXTRA));
    }

    public static SignupVia fromString(String s) {
        for (SignupVia v : values()) {
            if (v.name.equals(s)) return v;
        }
        return UNKNOWN;

    }

    public boolean isFacebook()  {
        return this == FACEBOOK_SSO || this == FACEBOOK_WEBFLOW;
    }
}
