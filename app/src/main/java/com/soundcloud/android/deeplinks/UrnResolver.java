package com.soundcloud.android.deeplinks;

import android.net.Uri;
import com.soundcloud.android.model.Urn;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class UrnResolver {

    private static final String SOUNDS_TYPE = "sounds|tracks";
    private static final String PLAYLISTS_TYPE = "playlists";
    private static final String USERS_TYPE = "users";

    private static final Pattern DEEP_LINK_PATTERN = Pattern.compile("^soundcloud://(" + SOUNDS_TYPE +
            "|" + PLAYLISTS_TYPE + "|" + USERS_TYPE + "):(\\d+).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern WEB_URN_PATTERN = Pattern.compile("^soundcloud:(" + SOUNDS_TYPE +
            "|" + PLAYLISTS_TYPE + "|" + USERS_TYPE + "):(\\d+).*", Pattern.CASE_INSENSITIVE);

    Urn toUrn(@NotNull Uri uri) {
        if (Urn.isValidUrn(uri)) {
            return Urn.parse(uri.toString());
        }

        Matcher deepLinkMatcher = DEEP_LINK_PATTERN.matcher(uri.toString());
        if (deepLinkMatcher.matches()) {
            return getUrn(deepLinkMatcher);
        }

        Matcher webLinkMatcher = WEB_URN_PATTERN.matcher(uri.toString());
        if (webLinkMatcher.matches()) {
            return getUrn(webLinkMatcher);
        }

        throw new IllegalArgumentException("Cannot parse as URN: " + uri);
    }

    private Urn getUrn(Matcher matcher) {
        return Urn.parse(Urn.SCHEME + ":" + matcher.group(1).toLowerCase() + ":" + matcher.group(2));
    }
}
