package com.soundcloud.android.activity.auth;

import android.content.Intent;
import android.os.Bundle;

public enum SignupVia {
    API("api"),
    FACEBOOK_SSO("facebook:access-token"),
    FACEBOOK_WEBFLOW("facebook:web-flow"),
    NONE("none");

    public static final String EXTRA = "signed_up";

    public final String name;

    private SignupVia(String name) {
        this.name = name;
    }

    public static SignupVia fromIntent(Intent intent) {
        return fromBundle(intent.getExtras());
    }

    public static SignupVia fromBundle(Bundle bundle) {
        return fromString(bundle.getString(EXTRA));
    }

    public static SignupVia fromString(String s) {
        for (SignupVia v : values()) {
            if (v.name.equals(s)) return v;
        }
        return NONE;

    }

    public boolean isFacebook()  {
        return this == FACEBOOK_SSO || this == FACEBOOK_WEBFLOW;
    }
}
