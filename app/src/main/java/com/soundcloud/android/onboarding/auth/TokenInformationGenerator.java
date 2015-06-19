package com.soundcloud.android.onboarding.auth;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.api.oauth.OAuth;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.onboarding.exceptions.TokenRetrievalException;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Map;

//TODO Move into TokenOperations
public class TokenInformationGenerator {
    private final ApiClient apiClient;
    private final OAuth oAuth;

    public interface TokenKeys {
        String EXTENSION_GRANT_TYPE_EXTRA = "extensionGrantType";
        String USERNAME_EXTRA = "username";
        String PASSWORD_EXTRA = "password";
    }

    @Inject
    public TokenInformationGenerator(ApiClient apiClient, OAuth oAuth) {
        this.apiClient = apiClient;
        this.oAuth = oAuth;
    }

    public Bundle getGrantBundle(String grantType, String token) {
        Bundle bundle = new Bundle();
        bundle.putString(TokenKeys.EXTENSION_GRANT_TYPE_EXTRA, grantType + token);
        return bundle;
    }

    public Token getToken(Bundle param) throws ApiRequestException {
        final ApiRequest request = ApiRequest.post(ApiEndpoints.OAUTH2_TOKEN.path())
                .forPublicApi()
                .withFormMap(getTokenParams(param))
                .build();

        final ApiResponse response = apiClient.fetchResponse(request);

        if (response.isNotSuccess()) {
            throw response.getFailure();
        }

        try {
            return new Token(new JSONObject(response.getResponseBody()));
        } catch (IOException | JSONException e) {
            throw new TokenRetrievalException(e);
        }
    }

    private Map<String, String> getTokenParams(Bundle data) {
        if (isFromUserCredentials(data)) {
            return oAuth.getTokenRequestParamsFromUserCredentials(getUsername(data), getPassword(data));
        } else if (isFromExtensionGrant(data)) {
            return oAuth.getTokenRequestParamsFromExtensionGrant(extractGrantType(data));
        } else {
            throw new IllegalArgumentException("invalid param " + data);
        }
    }

    public boolean isFromFacebook(Bundle data) {
        final String grantType = extractGrantType(data);
        return grantType != null && grantType.startsWith(OAuth.GRANT_TYPE_FACEBOOK);
    }

    private boolean isFromUserCredentials(Bundle data) {
        return data.containsKey(TokenKeys.USERNAME_EXTRA)
                && data.containsKey(TokenKeys.PASSWORD_EXTRA);
    }

    private boolean isFromExtensionGrant(Bundle data) {
        return data.containsKey(TokenKeys.EXTENSION_GRANT_TYPE_EXTRA);
    }

    private String extractGrantType(Bundle data) {
        return data.getString(TokenKeys.EXTENSION_GRANT_TYPE_EXTRA);
    }

    private String getUsername(Bundle data) {
        return data.getString(TokenKeys.USERNAME_EXTRA);
    }

    private String getPassword(Bundle data) {
        return data.getString(TokenKeys.PASSWORD_EXTRA);
    }
}
