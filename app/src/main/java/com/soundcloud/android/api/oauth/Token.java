package com.soundcloud.android.api.oauth;

import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

import android.support.annotation.VisibleForTesting;
import android.support.v4.util.ArrayMap;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Represents an OAuth2 access/refresh token pair.
 */
public class Token implements Serializable {

    public static final Token EMPTY = new Token(null, null);
    public static final String SCOPE_DEFAULT = "*";
    public static final String SCOPE_SIGNUP = "signup";
    public static final String SCOPE_NON_EXPIRING = "non-expiring";
    public static final String SCOPE_PLAYCOUNT = "playcount";

    private static final long serialVersionUID = 766168501082045382L;

    private static final String ACCESS_TOKEN = "access_token";
    private static final String REFRESH_TOKEN = "refresh_token";
    private static final String SCOPE = "scope";
    private static final String EXPIRES_IN = "expires_in";

    private String access, refresh;
    private long expiresAt;
    @Nullable private String scope;

    private final Map<String, String> customParameters = new ArrayMap<>();

    /**
     * Constructs a new token with the given sub-tokens
     *
     * @param access  A token used by the client to make authenticated requests on behalf of the resource owner.
     * @param refresh A token used by the client to obtain a new access token without having
     *                to involve the resource owner.
     */
    public Token(String access, String refresh) {
        this(access, refresh, null);
    }

    public Token(String access, String refresh, @Nullable String scope) {
        this.access = access;
        this.refresh = refresh;
        this.scope = scope;
    }

    @VisibleForTesting
    public Token(String access, String refresh, @Nullable String scope, long expiresAt) {
        this(access, refresh, scope);
        this.expiresAt = expiresAt;
    }

    /**
     * Construct a new token from a JSON response
     *
     * @param json the json response
     * @throws IOException JSON format error
     */
    public Token(JSONObject json) throws IOException {
        try {
            for (Iterator it = json.keys(); it.hasNext(); ) {
                final String key = it.next().toString();
                if (ACCESS_TOKEN.equals(key)) {
                    access = json.getString(key);
                } else if (REFRESH_TOKEN.equals(key)) {
                    // refresh token won't be set if we don't expire
                    refresh = json.getString(key);
                } else if (EXPIRES_IN.equals(key)) {
                    final long now = System.currentTimeMillis();
                    expiresAt = now + TimeUnit.SECONDS.toMillis(json.getLong(key));
                } else if (SCOPE.equals(key)) {
                    scope = json.getString(key);
                } else {
                    // custom parameter
                    customParameters.put(key, json.get(key).toString());
                }
            }
        } catch (JSONException e) {
            throw new IOException(e.getMessage());
        }
    }

    public String getAccessToken() {
        return access;
    }

    public String getRefreshToken() {
        return refresh;
    }

    public boolean hasRefreshToken() {
        return refresh != null;
    }

    @Nullable
    public String getScope() {
        return scope;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public String getParameter(String key) {
        return customParameters.get(key);
    }

    public void invalidate() {
        this.access = null;
    }

    public boolean hasDefaultScope() {
        return hasScope(SCOPE_DEFAULT);
    }

    public boolean hasScope(String scope) {
        if (this.scope != null) {
            for (String s : this.scope.split(" ")) {
                if (scope.equals(s)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean valid() {
        return access != null && (hasScope(SCOPE_NON_EXPIRING) || refresh != null);
    }

    /**
     * indicates whether this token was issued after a signup
     */
    public String getSignup() {
        return customParameters.get("soundcloud:user:sign-up");
    }

    @Override
    public String toString() {
        return "Token{" +
                "access='" + access + '\'' +
                ", refresh='" + refresh + '\'' +
                ", scope='" + scope + '\'' +
                ", expires=" + (expiresAt == 0 ? "never" : new Date(expiresAt)) +
                '}';
    }

    @Override @SuppressWarnings("PMD.ModifiedCyclomaticComplexity")
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }

        if (o instanceof Token) {
            Token token = (Token) o;
            if (access != null ? !access.equals(token.access) : token.access != null) {
                return false;
            }
            if (refresh != null ? !refresh.equals(token.refresh) : token.refresh != null) {
                return false;
            }
            if (scope != null ? !scope.equals(token.scope) : token.scope != null) {
                return false;
            }
            return true;
        } else {
            return super.equals(o);
        }
    }

    @Override
    public int hashCode() {
        int result = access != null ? access.hashCode() : 0;
        result = 31 * result + (refresh != null ? refresh.hashCode() : 0);
        result = 31 * result + (scope != null ? scope.hashCode() : 0);
        return result;
    }
}
