package com.soundcloud.android.analytics.playcounts;

import com.soundcloud.android.api.APIEndpoints;
import com.soundcloud.android.api.HttpProperties;
import com.soundcloud.android.events.PlaybackSessionEvent;

import android.net.Uri;

import javax.inject.Inject;

class PlayCountUrlBuilder {

    private static final String PUBLIC_API_BASE_URI = "https://api.soundcloud.com";

    private final HttpProperties httpProperties;

    @Inject
    PlayCountUrlBuilder(HttpProperties httpProperties) {
        this.httpProperties = httpProperties;
    }

    String buildUrl(PlaybackSessionEvent playbackSessionEvent) {
        final String trackId = Long.toString(playbackSessionEvent.getTrackUrn().numericId);
        final Uri.Builder builder = Uri.parse(PUBLIC_API_BASE_URI + APIEndpoints.LOG_PLAY.unencodedPath(trackId))
                .buildUpon()
                .appendQueryParameter("client_id", httpProperties.getClientId());

        final String policy = playbackSessionEvent.getTrackPolicy();
        if (policy != null) {
            builder.appendQueryParameter("policy", policy);
        }
        return builder.toString();
    }

}
