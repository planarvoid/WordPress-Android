package com.soundcloud.android.playback.service;

import static com.google.common.base.Preconditions.checkState;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.APIEndpoints;
import com.soundcloud.android.api.HttpProperties;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.api.Token;

import android.net.Uri;

import javax.inject.Inject;

public class PlaybackServiceOperations {

    private final AccountOperations accountOperations;
    private final HttpProperties httpProperties;

    @Inject
    public PlaybackServiceOperations(AccountOperations accountOperations, HttpProperties httpProperties) {
        this.accountOperations = accountOperations;
        this.httpProperties = httpProperties;
    }

    public String buildHLSUrlForTrack(TrackUrn urn) {
        checkState(accountOperations.isUserLoggedIn(), "SoundCloud User account does not exist");
        Token token = accountOperations.getSoundCloudToken();
        return Uri.parse(httpProperties.getPrivateApiHostWithHttpScheme() + APIEndpoints.HLS_STREAM.unencodedPath(urn))
                .buildUpon()
                .appendQueryParameter(HttpProperties.Parameter.OAUTH_PARAMETER.toString(), token.access)
                .build().toString();
    }
}
