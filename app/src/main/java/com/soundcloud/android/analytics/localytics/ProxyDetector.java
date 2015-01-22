package com.soundcloud.android.analytics.localytics;

import com.google.common.collect.ImmutableSet;

import javax.inject.Inject;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.util.List;

public class ProxyDetector {

    private final ProxySelector proxySelector;

    @Inject
    public ProxyDetector(ProxySelector proxySelector) {
        this.proxySelector = proxySelector;
    }

    public boolean isProxyConfiguredFor(URI uri) {
        final List<Proxy> proxies = proxySelector.select(uri);
        proxies.removeAll(ImmutableSet.of(Proxy.NO_PROXY));
        return !proxies.isEmpty();
    }
}
