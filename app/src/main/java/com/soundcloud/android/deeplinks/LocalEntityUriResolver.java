package com.soundcloud.android.deeplinks;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.model.UrnCollection;
import com.soundcloud.android.model.UrnNamespace;
import com.soundcloud.java.checks.Preconditions;
import com.soundcloud.java.optional.Optional;
import io.reactivex.Single;

import android.net.Uri;

import javax.inject.Inject;
import java.util.EnumSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class LocalEntityUriResolver {

    private static final Pattern URI_WEB_LINK_PATTERN = Pattern.compile("^(http[s]?)?(://)(www\\.)?(m\\.)?(soundcloud\\.com/)(.+)");
    private static final Pattern URI_APP_LINK_PATTERN = Pattern.compile("^soundcloud:(//)?(.+)");
    private static final Pattern URI_PATH_PATTERN = Pattern.compile("^([a-z\\-]+)[/:]([^/?]+)$");
    private static final Pattern URI_ID_PATTERN = Pattern.compile("^\\d+$");
    private static final EnumSet<UrnCollection> URI_LOCALLY_SUPPORTED_ENTITIES = EnumSet.of(UrnCollection.TRACKS,
                                                                                            UrnCollection.PLAYLISTS,
                                                                                            UrnCollection.USERS,
                                                                                            UrnCollection.SYSTEM_PLAYLIST);

    @Inject
    LocalEntityUriResolver() {
    }

    Single<Urn> resolve(String identifier) {
        Optional<String> path = extractPath(identifier);
        Preconditions.checkState(path.isPresent(), "canResolveLocally should be called before to verify the URN can be extracted");

        return path.transform(pathToMatch -> {
            final Matcher pathMatcher = URI_PATH_PATTERN.matcher(pathToMatch);
            Preconditions.checkState(pathMatcher.matches(), "canResolveLocally should be called before to verify the URN can be extracted");
            final String entity = pathMatcher.group(1);
            final String pathId = pathMatcher.group(2);
            final Urn urn;
            if (URI_ID_PATTERN.matcher(pathId).matches()) {
                urn = new Urn(UrnNamespace.SOUNDCLOUD, UrnCollection.from(entity), Long.valueOf(pathId));
            } else if (pathId.startsWith(UrnNamespace.SOUNDCLOUD.value())){
                urn = new Urn(pathId);
            } else {
                urn = new Urn(UrnNamespace.SOUNDCLOUD, UrnCollection.from(entity), pathId);
            }
            return Single.just(urn);
        }).get();
    }

    boolean canResolveLocally(String identifier) {
        final Optional<String> path = extractPath(identifier);

        return path.transform(pathToMatch -> {
            Matcher pathMatcher = URI_PATH_PATTERN.matcher(pathToMatch);
            if (!pathMatcher.matches()) {
                return false;
            }
            String entity = pathMatcher.group(1);
            return canResolveLocally(UrnCollection.from(entity));
        }).or(false);
    }

    boolean canResolveLocally(Urn urn) {
        return canResolveLocally(UrnCollection.from(urn));
    }

    private boolean canResolveLocally(UrnCollection urnCollection) {
        return URI_LOCALLY_SUPPORTED_ENTITIES.contains(urnCollection);
    }

    boolean isKnownDeeplink(String identifier) {
        DeepLink deepLink = DeepLink.fromUri(Uri.parse(identifier));
        return !deepLink.requiresResolve();
    }

    private Optional<String> extractPath(String identifier) {
        Matcher webLinkMatcher = URI_WEB_LINK_PATTERN.matcher(identifier);
        Matcher appLinkMatcher = URI_APP_LINK_PATTERN.matcher(identifier);

        final String path;
        if (webLinkMatcher.matches()) {
            path = webLinkMatcher.group(6);
        } else if (appLinkMatcher.matches()) {
            path = appLinkMatcher.group(2);
        } else {
            path = null;
        }
        return Optional.fromNullable(path);
    }

}
