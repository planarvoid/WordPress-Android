package com.soundcloud.android.deeplinks;

import android.net.Uri;

import java.util.regex.Pattern;

class WebUrlResolver {

    private static final Pattern[] VALID_PATH_PATTERNS = {
            Pattern.compile("^/login/reset/[0-9a-f]+$"),
            Pattern.compile("^/emails/[0-9a-f]+$"),
            Pattern.compile("^/pages/.*$"),
            Pattern.compile("^/terms-of-use$"),
            Pattern.compile("^/jobs(/.*)?$")
    };

    public static boolean shouldOpenInBrowser(Uri uri) {
        String scheme = uri.getScheme();

        if ("http".equals(scheme) || "https".equals(scheme)) {
            for (Pattern pattern : VALID_PATH_PATTERNS) {
                if (pattern.matcher(uri.getPath()).matches()) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean isClickTrackingUrl(Uri uri) {
        return "soundcloud.com".equals(uri.getHost()) && uri.getPath().startsWith("/-/t/click");
    }
}
