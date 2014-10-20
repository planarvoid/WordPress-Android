package com.soundcloud.android.playback.service;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.HttpProperties;
import com.soundcloud.android.model.Urn;
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

    public String buildHLSUrlForTrack(Urn urn) {
        checkState(accountOperations.isUserLoggedIn(), "SoundCloud User account does not exist");
        Token token = checkNotNull(accountOperations.getSoundCloudToken(), "The SoundCloud token should not be null");
        return Uri.parse(httpProperties.getPrivateApiHostWithHttpScheme() + ApiEndpoints.HLS_STREAM.unencodedPath(urn))
                .buildUpon()
                .appendQueryParameter(HttpProperties.Parameter.OAUTH_PARAMETER.toString(), token.access)
                .build().toString();
    }
}
