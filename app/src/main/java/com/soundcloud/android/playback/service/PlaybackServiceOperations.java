package com.soundcloud.android.playback.service;

import static com.google.common.base.Preconditions.checkState;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.APIEndpoints;
import com.soundcloud.android.api.APIRequest;
import com.soundcloud.android.api.APIResponse;
import com.soundcloud.android.api.HttpProperties;
import com.soundcloud.android.api.RxHttpClient;
import com.soundcloud.android.api.SoundCloudAPIRequest;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.api.Token;
import rx.Observable;
import rx.functions.Func1;

import android.net.Uri;

import javax.inject.Inject;

public class PlaybackServiceOperations {

    private static final String PARAM_CLIENT_ID = "client_id";

    private final AccountOperations accountOperations;
    private final HttpProperties httpProperties;
    private final RxHttpClient rxHttpClient;

    @Inject
    public PlaybackServiceOperations(AccountOperations accountOperations, HttpProperties httpProperties, RxHttpClient rxHttpClient) {
        this.accountOperations = accountOperations;
        this.httpProperties = httpProperties;
        this.rxHttpClient = rxHttpClient;
    }

    public String buildHLSUrlForTrack(PublicApiTrack track) {
        checkState(accountOperations.isUserLoggedIn(), "SoundCloud User account does not exist");
        Token token = accountOperations.getSoundCloudToken();
        return Uri.parse(httpProperties.getPrivateApiHostWithHttpScheme() + APIEndpoints.HLS_STREAM.unencodedPath(track.getUrn()))
                .buildUpon()
                .appendQueryParameter(HttpProperties.Parameter.OAUTH_PARAMETER.toString(), token.access)
                .build().toString();
    }

    public Observable<TrackUrn> logPlay(final TrackUrn urn){
        final APIRequest apiRequest = buildRequestForLoggingPlay(urn);
        return rxHttpClient.fetchResponse(apiRequest).map(new Func1<APIResponse, TrackUrn>() {
            @Override
            public TrackUrn call(APIResponse apiResponse) {
                return urn;
            }
        });
    }

    private APIRequest buildRequestForLoggingPlay(final TrackUrn trackUrn) {
        final String endpoint = String.format(APIEndpoints.LOG_PLAY.path(), trackUrn.toEncodedString());
        SoundCloudAPIRequest.RequestBuilder builder = SoundCloudAPIRequest.RequestBuilder.post(endpoint);
        builder.addQueryParameters(PARAM_CLIENT_ID, httpProperties.getClientId());
        return builder.forPrivateAPI(1).build();
    }
}
