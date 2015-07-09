package com.soundcloud.android.deeplinks;

import com.soundcloud.android.model.Urn;
import org.jetbrains.annotations.NotNull;

import android.net.Uri;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class UrnResolver {

    private static final String SOUNDS_TYPE = "sounds|tracks";
    private static final String PLAYLISTS_TYPE = "playlists";
    private static final String USERS_TYPE = "users";
    private static final String ALL_TYPES = SOUNDS_TYPE + "|" + PLAYLISTS_TYPE + "|" + USERS_TYPE;

    private static final Pattern DEEP_LINK_PATTERN = Pattern.compile("^soundcloud://(" + ALL_TYPES + "):(\\d+).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern WEB_URN_PATTERN = Pattern.compile("^soundcloud:(" + ALL_TYPES + "):(\\d+).*", Pattern.CASE_INSENSITIVE);

    Urn toUrn(@NotNull Uri uri) {
        final String urnString = uri.toString();
        if (Urn.isSoundCloudUrn(urnString)) {
            return new Urn(urnString);
        }

        Matcher deepLinkMatcher = DEEP_LINK_PATTERN.matcher(urnString);
        if (deepLinkMatcher.matches()) {
            return getUrn(deepLinkMatcher);
        }

        Matcher webLinkMatcher = WEB_URN_PATTERN.matcher(urnString);
        if (webLinkMatcher.matches()) {
            return getUrn(webLinkMatcher);
        }

        throw new IllegalArgumentException("Cannot parse as URN: " + uri);
    }

    private Urn getUrn(Matcher matcher) {
        String type = matcher.group(1).toLowerCase(Locale.US);
        if (type.equals("sounds")) {
            type = "tracks";
        }
        return new Urn(Urn.SOUNDCLOUD_SCHEME + ":" + type + ":" + matcher.group(2));
    }
}
