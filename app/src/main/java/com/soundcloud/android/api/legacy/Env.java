package com.soundcloud.android.api.legacy;

import org.apache.http.HttpHost;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * The environment to operate against.
 */
public enum Env {
    /**
     * The main production site, http://soundcloud.com
     */
    LIVE("api.soundcloud.com", "soundcloud.com");

    private final HttpHost sslResourceHost, sslAuthResourceHost;

    /**
     * @param resourceHost     the resource host
     * @param authResourceHost the authentication resource host
     */
    Env(String resourceHost, String authResourceHost) {
        sslResourceHost = new HttpHost(resourceHost, 443, "https");
        sslAuthResourceHost = new HttpHost(authResourceHost, 443, "https");
    }

    public HttpHost getSecureResourceHost() {
        return sslResourceHost;
    }

    /* package */ HttpHost getSecureAuthResourceHost() {
        return sslAuthResourceHost;
    }

    /* package */ URI getSecureAuthResourceURI() {
        return hostToUri(getSecureAuthResourceHost());
    }

    /* package */
    public boolean isApiHost(HttpHost host) {
        return ("http".equals(host.getSchemeName()) ||
                "https".equals(host.getSchemeName())) &&
                sslResourceHost.getHostName().equals(host.getHostName());
    }

    private static URI hostToUri(HttpHost host) {
        try {
            return new URI(host.getSchemeName(), host.getHostName(), null, null);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
