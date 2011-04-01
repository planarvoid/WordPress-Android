package com.soundcloud.api;

import org.apache.http.FormattedHeader;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpRequest;
import org.apache.http.auth.AUTH;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthSchemeFactory;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.MalformedChallengeException;
import org.apache.http.message.BasicHeaderValueParser;
import org.apache.http.message.HeaderValueParser;
import org.apache.http.message.ParserCursor;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.CharArrayBuffer;

import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;

public class OAuthScheme implements AuthScheme {
    public HashMap<String, String> mParams;
    public HttpParams mHttpParams;
    private CloudAPI mApi;

    public OAuthScheme(CloudAPI api, HttpParams params) {
        mApi = api;
        mHttpParams = params;
        mParams = new HashMap<String, String>();
    }

    @Override
    public String getSchemeName() {
        return CloudAPI.OAUTH_SCHEME;
    }

    @Override
    public String getParameter(String name) {
        return mParams.get(name);
    }

    @Override
    public String getRealm() {
        return getParameter("realm");
    }

    @Override
    public boolean isConnectionBased() {
        return false;
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    @Override
    public Header authenticate(Credentials credentials, HttpRequest request) throws AuthenticationException {
        mApi.invalidateToken();
        try {
            return ApiWrapper.getOAuthHeader(mApi.refreshToken().getToken());
        } catch (IOException e) {
            throw new AuthenticationException("Error refreshing token", e);
        } catch (IllegalStateException e) {
            throw new AuthenticationException("Error refreshing token", e);
        }
    }

    @Override
    public void processChallenge(Header header) throws MalformedChallengeException {
        if (header == null) {
            throw new IllegalArgumentException("Header may not be null");
        }
        String authheader = header.getName();
        if (!authheader.equalsIgnoreCase(AUTH.WWW_AUTH)) {
            throw new MalformedChallengeException("Unexpected header name: " + authheader);
        }

        CharArrayBuffer buffer;
        int pos;
        if (header instanceof FormattedHeader) {
            buffer = ((FormattedHeader) header).getBuffer();
            pos = ((FormattedHeader) header).getValuePos();
        } else {
            String s = header.getValue();
            if (s == null) {
                throw new MalformedChallengeException("Header value is null");
            }
            buffer = new CharArrayBuffer(s.length());
            buffer.append(s);
            pos = 0;
        }
        while (pos < buffer.length() && HTTP.isWhitespace(buffer.charAt(pos))) {
            pos++;
        }
        int beginIndex = pos;
        while (pos < buffer.length() && !HTTP.isWhitespace(buffer.charAt(pos))) {
            pos++;
        }
        int endIndex = pos;
        String s = buffer.substring(beginIndex, endIndex);
        if (!s.equalsIgnoreCase(getSchemeName())) {
            throw new MalformedChallengeException("Invalid scheme identifier: " + s);
        }
        HeaderValueParser parser = BasicHeaderValueParser.DEFAULT;
        ParserCursor cursor = new ParserCursor(pos, buffer.length());
        HeaderElement[] elements = parser.parseElements(buffer, cursor);
        if (elements.length == 0) {
            throw new MalformedChallengeException("Authentication challenge is empty");
        }
        for (HeaderElement element : elements) {
            this.mParams.put(element.getName(), element.getValue());
        }
    }

    static class Factory implements AuthSchemeFactory {
        private CloudAPI api;

        public Factory(CloudAPI api) {
            this.api = api;
        }

        @Override
        public AuthScheme newInstance(HttpParams params) {
            return new OAuthScheme(api, params);
        }
    }

    static class EmptyCredentials implements Credentials {
        public static final Credentials INSTANCE = new EmptyCredentials();
        @Override
        public Principal getUserPrincipal() {
            return null;
        }

        @Override
        public String getPassword() {
            return null;
        }
    }
}
