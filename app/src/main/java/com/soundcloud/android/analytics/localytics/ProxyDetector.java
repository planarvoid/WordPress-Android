package com.soundcloud.android.analytics.localytics;

import javax.inject.Inject;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProxyDetector {

    private final ProxySelector proxySelector;

    @Inject
    public ProxyDetector(ProxySelector proxySelector) {
        this.proxySelector = proxySelector;
    }

    public boolean isProxyConfiguredFor(URI uri) {
        if (uri.getScheme() == null) {
            return false;
        }

        List<Proxy> selectedProxies = proxySelector.select(uri);
        if (selectedProxies == null || selectedProxies.isEmpty()) {
            return false;
        }

        List<Proxy> proxies = new ArrayList<>(selectedProxies);
        proxies.removeAll(Collections.singleton(Proxy.NO_PROXY));
        return !proxies.isEmpty();
    }
}
