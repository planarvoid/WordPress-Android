package com.soundcloud.api;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class Token {
    public static final String ACCESS_TOKEN = "access_token";
    public static final String REFRESH_TOKEN = "refresh_token";
    public static final String SCOPE = "scope";
    public static final String EXPIRES_IN = "expires_in";

    public String access, refresh, scope;
    public long expiresIn;

    public Token(String access, String refresh) {
        this.access = access;
        this.refresh = refresh;
    }

    public Token(JSONObject json) throws JSONException {
        access = json.getString(ACCESS_TOKEN);
        refresh = json.getString(REFRESH_TOKEN);
        scope = json.getString(SCOPE);
        expiresIn = System.currentTimeMillis() + json.getLong(EXPIRES_IN) * 1000;
    }

    public void invalidate() {
        this.access = null;
    }

    public Date getExpiresIn() {
        return expiresIn == 0 ? null : new Date(expiresIn);
    }

    @Override
    public String toString() {
        return "Token{" +
                "access='" + access + '\'' +
                ", refresh='" + refresh + '\'' +
                ", scope='" + scope + '\'' +
                ", expires=" + getExpiresIn() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;

        if (o instanceof String) {
            return o.equals(access);
        } else if (o instanceof Token) {
            Token token = (Token) o;
            if (access != null ? !access.equals(token.access) : token.access != null) return false;
            if (refresh != null ? !refresh.equals(token.refresh) : token.refresh != null) return false;
            if (scope != null ? !scope.equals(token.scope) : token.scope != null) return false;
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
