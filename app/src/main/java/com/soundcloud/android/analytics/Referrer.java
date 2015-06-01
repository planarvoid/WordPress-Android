package com.soundcloud.android.analytics;

import com.soundcloud.android.Consts;
import org.jetbrains.annotations.NotNull;

import android.content.Intent;

import java.util.Locale;

public enum Referrer {
    OTHER("other"),
    HOME_BUTTON("home_button"),
    FACEBOOK("facebook"),
    TWITTER("twitter"),
    MOBI("mobi"),
    GOOGLE("google"),
    GOOGLE_PLUS("google_plus"),
    STREAM_NOTIFICATION("stream_notification"),
    ACTIVITIES_NOTIFICATION("activities_notification"),
    PLAYBACK_NOTIFICATION("playback_notification"),
    PLAYBACK_WIDGET("playback_widget"),
    GOOGLE_CRAWLER("google_crawler");

    private static final String HOST_GOOGLE = "google.com";
    private static final String HOST_FACEBOOK = "facebook.com";
    private static final String ORDINAL_EXTRA = "ReferrerOrdinal";

    private final String referrerTag;

    Referrer(String referrerTag) {
        this.referrerTag = referrerTag;
    }

    public String get() {
        return referrerTag;
    }

    public void addToIntent(Intent intent) {
        intent.putExtra(Referrer.ORDINAL_EXTRA, ordinal());
    }

    public static Referrer fromIntent(Intent intent) {
        return values()[intent.getIntExtra(Referrer.ORDINAL_EXTRA, 0)];
    }

    public static Referrer fromOrigin(@NotNull String referrer) {
        try {
            return Referrer.valueOf(referrer.toUpperCase(Locale.US));
        } catch (IllegalArgumentException ex) {
            return Referrer.OTHER;
        }
    }

    public static Referrer fromHost(@NotNull String host) {
        if (host.indexOf("www.") == 0) {
            host = host.substring(4);
        }

        switch (host) {
            case HOST_GOOGLE:
                return Referrer.GOOGLE;
            case HOST_FACEBOOK:
                return Referrer.FACEBOOK;
            default:
                return Referrer.OTHER;
        }
    }

    public static boolean hasReferrer(Intent intent) {
        return intent.hasExtra(Referrer.ORDINAL_EXTRA);
    }
}
