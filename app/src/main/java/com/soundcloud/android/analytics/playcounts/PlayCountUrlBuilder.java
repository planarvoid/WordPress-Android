package com.soundcloud.android.analytics.playcounts;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.HttpProperties;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.api.oauth.Token;

import android.net.Uri;

import javax.inject.Inject;

class PlayCountUrlBuilder {

    private static final String PUBLIC_API_BASE_URI = "https://api.soundcloud.com";

    private final HttpProperties httpProperties;
    private final AccountOperations accountOperations;

    @Inject
    PlayCountUrlBuilder(HttpProperties httpProperties, AccountOperations accountOperations) {
        this.httpProperties = httpProperties;
        this.accountOperations = accountOperations;
    }

    String buildUrl(PlaybackSessionEvent event) {
        final long trackId = new Urn(event.get(PlaybackSessionEvent.KEY_TRACK_URN)).getNumericId();
        final Uri.Builder builder = Uri.parse(PUBLIC_API_BASE_URI + ApiEndpoints.LOG_PLAY.unencodedPath(trackId))
                .buildUpon()
                .appendQueryParameter("client_id", httpProperties.getClientId());

        final Token token = accountOperations.getSoundCloudToken();
        if (token.valid()) {
            builder.appendQueryParameter("oauth_token", token.getAccessToken());
        }

        final String policy = event.get(PlaybackSessionEvent.KEY_POLICY);
        if (policy != null) {
            builder.appendQueryParameter("policy", policy);
        }
        return builder.toString();
    }

}
