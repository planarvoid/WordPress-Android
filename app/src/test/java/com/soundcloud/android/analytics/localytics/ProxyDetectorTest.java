package com.soundcloud.android.analytics.localytics;

import static com.google.common.collect.Lists.newArrayList;
import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;

@RunWith(SoundCloudTestRunner.class)
public class ProxyDetectorTest {

    private static final URI URI = java.net.URI.create("http://soundcloud.com");
    private static final Proxy A_PROXY = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("some.proxy", 8888));
    @Mock private ProxySelector proxySelector;
    private ProxyDetector proxyDetector;

    @Before
    public void setup() {
        proxyDetector = new ProxyDetector(proxySelector);
    }

    @Test
    public void noProxyIsNotAProxy() {
        when(proxySelector.select(URI)).thenReturn(newArrayList(Proxy.NO_PROXY));

        expect(proxyDetector.isProxyConfiguredFor(URI)).toBeFalse();
    }

    @Test
    public void someProxyIsAProxy() {
        when(proxySelector.select(URI)).thenReturn(newArrayList(A_PROXY));

        expect(proxyDetector.isProxyConfiguredFor(URI)).toBeTrue();
    }

    @Test
    public void mixedListIsAProxy() {
        when(proxySelector.select(URI)).thenReturn(newArrayList(A_PROXY, Proxy.NO_PROXY));

        expect(proxyDetector.isProxyConfiguredFor(URI)).toBeTrue();
    }

    @Test
    public void noListIsNotAProxy() {
        when(proxySelector.select(URI)).thenReturn(Lists.<Proxy>newArrayList());

        expect(proxyDetector.isProxyConfiguredFor(URI)).toBeFalse();
    }

    @Test
    public void multipleNoProxiesAreStillNotAProxy() {
        when(proxySelector.select(URI)).thenReturn(newArrayList(Proxy.NO_PROXY, Proxy.NO_PROXY, Proxy.NO_PROXY));

        expect(proxyDetector.isProxyConfiguredFor(URI)).toBeFalse();
    }

}
