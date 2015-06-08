package com.soundcloud.android.playback;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiUrlBuilder;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.model.Urn;

import android.util.Log;

import javax.inject.Inject;

public class StreamUrlBuilder {

    private final static String TAG = StreamUrlBuilder.class.getSimpleName();
    private final AccountOperations accountOperations;
    private final ApiUrlBuilder urlBuilder;

    @Inject
    StreamUrlBuilder(AccountOperations accountOperations, ApiUrlBuilder urlBuilder){
        this.accountOperations = accountOperations;
        this.urlBuilder = urlBuilder;
    }

    public String buildHttpStreamUrl(Urn trackUrn) {
        Token token = accountOperations.getSoundCloudToken();
        return urlBuilder.from(ApiEndpoints.HTTP_STREAM, trackUrn)
                .withQueryParam(ApiRequest.Param.OAUTH_TOKEN, token.getAccessToken())
                .build();
    }

    public String buildHttpsStreamUrl(Urn trackUrn) {
        Token token = accountOperations.getSoundCloudToken();
        Log.d(TAG, "token missing? " + (token.getAccessToken() != null));
        return urlBuilder.from(ApiEndpoints.HTTPS_STREAM, trackUrn)
                .withQueryParam(ApiRequest.Param.OAUTH_TOKEN, token.getAccessToken())
                .build();
    }


}
