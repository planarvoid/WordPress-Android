package com.soundcloud.android.stations;

import com.soundcloud.android.deeplinks.DeepLink;
import com.soundcloud.android.deeplinks.UriResolveException;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.model.UrnNamespace;
import com.soundcloud.java.optional.Optional;

import android.net.Uri;

import javax.inject.Inject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StationsUriResolver {

    private static final Pattern ID_PATTERN = Pattern.compile("\\d+");
    /**
     * Supports parsing `artist-stations` and `track-stations` URNs.
     * Since web schema supports also `/stations/<artist|track>/<user|track|station urn>`, we can also parse those URNs here.
     */
    private static final Pattern URN_PATTERN = Pattern.compile(UrnNamespace.SOUNDCLOUD + ":(users|tracks|((artist|track)-stations)):(\\d+)");

    @Inject
    public StationsUriResolver() {
    }

    public Optional<Urn> resolve(Uri uri) throws UriResolveException {
        try {
            final String path;
            if (DeepLink.isWebScheme(uri)) {
                path = extractPathFromWebScheme(uri);
            } else if (DeepLink.isHierarchicalSoundCloudScheme(uri)) {
                path = extractPathFromSoundCloudScheme(uri);
            } else {
                throw new IllegalArgumentException("Invalid schema for stations deeplink");
            }

            return extractUrn(path);
        } catch (Exception e) {
            throw new UriResolveException("Station uri " + uri + " could not be resolved", e);
        }
    }

    private String extractPathFromSoundCloudScheme(Uri uri) {
        return uri.getPath();
    }

    private String extractPathFromWebScheme(Uri uri) {
        return uri.getPath().replaceFirst("/stations", "");
    }

    private Optional<Urn> extractUrn(String path) throws IllegalArgumentException {
        if (path.startsWith("/artist/")) {
            return extractArtistStationUrn(path.replaceFirst("/artist/", ""));
        } else if (path.startsWith("/track/")) {
            return extractTrackStationUrn(path.replaceFirst("/track/", ""));
        } else {
            throw new IllegalArgumentException("Invalid schema for stations deeplink with path: " + path);
        }
    }

    private Optional<Urn> extractTrackStationUrn(String path) {
        return extractId(path).transform(Urn::forTrackStation);
    }

    private Optional<Urn> extractArtistStationUrn(String path) {
        return extractId(path).transform(Urn::forArtistStation);
    }

    private Optional<Long> extractId(String path) {
        Matcher idMatcher = ID_PATTERN.matcher(path);
        Matcher urnMatcher = URN_PATTERN.matcher(path);
        if (idMatcher.matches()) {
            return Optional.of(Long.parseLong(path));
        } else if (urnMatcher.matches()) {
            return Optional.of(Long.parseLong(urnMatcher.group(urnMatcher.groupCount())));
        } else {
            return Optional.absent();
        }
    }
}
