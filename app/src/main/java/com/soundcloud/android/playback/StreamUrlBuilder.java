package com.soundcloud.android.playback;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiUrlBuilder;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.model.Urn;

import javax.inject.Inject;

public class StreamUrlBuilder {

    private final static String TAG = StreamUrlBuilder.class.getSimpleName();
    private final AccountOperations accountOperations;
    private final ApiUrlBuilder urlBuilder;

    @Inject
    StreamUrlBuilder(AccountOperations accountOperations, ApiUrlBuilder urlBuilder) {
        this.accountOperations = accountOperations;
        this.urlBuilder = urlBuilder;
    }

    public String buildHttpStreamUrl(Urn trackUrn) {
        ApiUrlBuilder apiUrlBuilder = urlBuilder.from(ApiEndpoints.HTTP_STREAM, trackUrn);
        return addTokenWhenValid(apiUrlBuilder).build();
    }

    public String buildHttpsStreamUrl(Urn trackUrn) {
        ApiUrlBuilder apiUrlBuilder = urlBuilder.from(ApiEndpoints.HTTPS_STREAM, trackUrn);
        return addTokenWhenValid(apiUrlBuilder).build();
    }

    private ApiUrlBuilder addTokenWhenValid(ApiUrlBuilder urlBuilder) {
        Token token = accountOperations.getSoundCloudToken();
        if (token.valid()) {
            urlBuilder.withQueryParam(ApiRequest.Param.OAUTH_TOKEN, token.getAccessToken());
        }
        return urlBuilder;
    }
}
