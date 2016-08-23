package com.soundcloud.android.playback;

import android.net.Uri;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AudioAdSource;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiUrlBuilder;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.model.Urn;

import javax.inject.Inject;

public class StreamUrlBuilder {

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

    public String buildAdUrlWithAuth(AudioAdSource source) {
        Uri.Builder builder = Uri.parse(source.getUrl()).buildUpon();

        Token token = accountOperations.getSoundCloudToken();
        if (token.valid()) {
            builder.appendQueryParameter(ApiRequest.Param.OAUTH_TOKEN.toString(), token.getAccessToken());
        }

        return builder.build().toString();
    }
}
