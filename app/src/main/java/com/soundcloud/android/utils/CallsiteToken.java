package com.soundcloud.android.utils;

// marker type that leverages Throwable to capture a call site stack trace
public final class CallsiteToken extends Throwable {

    public static CallsiteToken build() {
        return new CallsiteToken();
    }

    private CallsiteToken() {
        // don't construct me manually
    }
}
