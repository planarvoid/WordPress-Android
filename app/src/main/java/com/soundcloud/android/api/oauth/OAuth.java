package com.soundcloud.android.api.oauth;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.HttpProperties;
import org.apache.http.Header;
import org.apache.http.auth.AUTH;
import org.apache.http.message.BasicHeader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.support.v4.util.ArrayMap;
import android.text.TextUtils;

import javax.inject.Inject;
import java.net.URI;
import java.util.Map;

public class OAuth {
    public static final URI REDIRECT_URI = URI.create("soundcloud://auth");
    public static final String[] DEFAULT_SCOPES = {Token.SCOPE_NON_EXPIRING};

    // OAuth2 parameters
    public static final String PARAM_GRANT_TYPE = "grant_type";
    public static final String PARAM_CLIENT_ID = "client_id";
    public static final String PARAM_CLIENT_SECRET = "client_secret";
    public static final String PARAM_USERNAME = "username";
    public static final String PARAM_PASSWORD = "password";
    public static final String PARAM_REDIRECT_URI = "redirect_uri";
    public static final String PARAM_RESPONSE_TYPE = "response_type";
    public static final String PARAM_CODE = "code";
    public static final String PARAM_SCOPE = "scope";
    public static final String PARAM_REFRESH_TOKEN = "refresh_token";

    // parameter value constants
    public static final String RESPONSE_TYPE_CODE = "code";

    // oauth2 grant types
    public static final String GRANT_TYPE_PASSWORD = "password";
    public static final String GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";
    public static final String GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials";
    public static final String GRANT_TYPE_REFRESH_TOKEN = "refresh_token";
    public static final String GRANT_TYPE_FACEBOOK = "urn:soundcloud:oauth2:grant-type:facebook&access_token="; // oauth2 extension
    public static final String GRANT_TYPE_GOOGLE_PLUS = "urn:soundcloud:oauth2:grant-type:google_plus&access_token=";

    private static final Token EMPTY_TOKEN = new Token(null, null);

    private final String clientId;
    private final String clientSecret;

    @NotNull private Token token;

    @Inject
    public OAuth(HttpProperties httpProperties, AccountOperations accountOperations) {
        this(httpProperties.getClientId(), httpProperties.getClientSecret(),
                accountOperations.getSoundCloudToken());
    }

    @Deprecated // remove along with ApiWrapper
    public OAuth(String clientId, String clientSecret, @Nullable Token token) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.token = token == null ? EMPTY_TOKEN : token;
    }

    public String getClientId() {
        return clientId;
    }

    @NotNull
    public Token getToken() {
        return token;
    }

    public boolean hasToken() {
        return token != EMPTY_TOKEN;
    }

    // This shouldn't be required anymore, since after logging in, any ApiClient instance
    // will have its own OAuth instance which in turn gets the current token injected
    @Deprecated
    public void setToken(@Nullable Token newToken) {
        token = newToken == null ? EMPTY_TOKEN : newToken;
    }

    /**
     * Creates an OAuth2 header for the given token
     */
    @Deprecated
    public static Header createOAuthHeader(Token token) {
        return new BasicHeader(AUTH.WWW_AUTH_RESP, "OAuth " +
                (token == null || !token.valid() ? "invalidated" : token.getAccessToken()));
    }

    public String getAuthorizationHeaderValue() {
        return "OAuth " + (token.valid() ? token.getAccessToken() : "invalidated");
    }

    public Map<String, String> getTokenRequestParamsFromUserCredentials(String username, String password) {
        final ArrayMap<String, String> params = new ArrayMap<>(6);
        params.put(OAuth.PARAM_GRANT_TYPE, OAuth.GRANT_TYPE_PASSWORD);
        params.put(OAuth.PARAM_CLIENT_ID, clientId);
        params.put(OAuth.PARAM_CLIENT_SECRET, clientSecret);
        params.put(OAuth.PARAM_USERNAME, username);
        params.put(OAuth.PARAM_PASSWORD, password);
        params.put(OAuth.PARAM_SCOPE, getScopeParam(DEFAULT_SCOPES));
        return params;
    }

    public Map<String, String> getTokenRequestParamsFromCode(String code) {
        final ArrayMap<String, String> params = new ArrayMap<>(6);
        params.put(OAuth.PARAM_GRANT_TYPE, OAuth.GRANT_TYPE_AUTHORIZATION_CODE);
        params.put(OAuth.PARAM_CLIENT_ID, clientId);
        params.put(OAuth.PARAM_CLIENT_SECRET, clientSecret);
        params.put(OAuth.PARAM_REDIRECT_URI, REDIRECT_URI.toString());
        params.put(OAuth.PARAM_CODE, code);
        params.put(OAuth.PARAM_SCOPE, getScopeParam(DEFAULT_SCOPES));
        return params;
    }

    public Map<String, String> getTokenRequestParamsFromClientCredentials(String... scopes) {
        final ArrayMap<String, String> params = new ArrayMap<>(4);
        params.put(OAuth.PARAM_GRANT_TYPE, OAuth.GRANT_TYPE_CLIENT_CREDENTIALS);
        params.put(OAuth.PARAM_CLIENT_ID, clientId);
        params.put(OAuth.PARAM_CLIENT_SECRET, clientSecret);
        if (scopes.length > 0) {
            params.put(OAuth.PARAM_SCOPE, getScopeParam(scopes));
        }
        return params;
    }

    public Map<String, String> getTokenRequestParamsFromExtensionGrant(String grantType) {
        final ArrayMap<String, String> params = new ArrayMap<>(4);
        params.put(OAuth.PARAM_GRANT_TYPE, grantType);
        params.put(OAuth.PARAM_CLIENT_ID, clientId);
        params.put(OAuth.PARAM_CLIENT_SECRET, clientSecret);
        params.put(OAuth.PARAM_SCOPE, getScopeParam(DEFAULT_SCOPES));
        return params;
    }

    public Map<String, String> getTokenRequestParamsForRefreshToken() {
        final ArrayMap<String, String> params = new ArrayMap<>(4);
        params.put(OAuth.PARAM_GRANT_TYPE, GRANT_TYPE_REFRESH_TOKEN);
        params.put(OAuth.PARAM_CLIENT_ID, clientId);
        params.put(OAuth.PARAM_CLIENT_SECRET, clientSecret);
        params.put(OAuth.PARAM_REFRESH_TOKEN, token.getRefreshToken());
        return params;
    }

    private String getScopeParam(String... scopes) {
        return TextUtils.join(" ", scopes);
    }
}
