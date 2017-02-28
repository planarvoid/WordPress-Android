package com.soundcloud.android.analytics;

import com.soundcloud.java.strings.Strings;
import org.jetbrains.annotations.NotNull;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;

import java.util.Locale;

public enum Referrer {
    OTHER("other"),
    HOME_BUTTON("home_button"),
    FACEBOOK("facebook"),
    TWITTER("twitter"),
    MOBI("mobi"),
    GOOGLE_PLUS("google_plus"),
    APPBOY_NOTIFICATION("appboy_notification"),
    STREAM_NOTIFICATION("stream_notification"),
    ACTIVITIES_NOTIFICATION("activities_notification"),
    PLAYBACK_NOTIFICATION("playback_notification"),
    PLAYBACK_WIDGET("playback_widget"),
    LAUNCHER_SHORTCUT("launcher_shortcut"),
    GOOGLE_CRAWLER("google_crawler");

    private static final String ORDINAL_EXTRA = "ReferrerOrdinal";

    private final String referrerTag;

    Referrer(String referrerTag) {
        this.referrerTag = referrerTag;
    }

    public String value() {
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

    public static String fromUrl(@Nullable String url) {
        if (url != null) {
            String host = Uri.parse(url).getHost();
            if (Strings.isNotBlank(host)) {
                if (host.indexOf("www.") == 0) {
                    host = host.substring(4);
                }
                return host;
            }
        }
        return Referrer.OTHER.value();
    }

    public static boolean hasReferrer(Intent intent) {
        return intent.hasExtra(Referrer.ORDINAL_EXTRA);
    }
}
