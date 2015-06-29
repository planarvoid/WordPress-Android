package com.soundcloud.android.analytics.localytics;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;

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
        when(proxySelector.select(URI)).thenReturn(Arrays.asList(Proxy.NO_PROXY));

        expect(proxyDetector.isProxyConfiguredFor(URI)).toBeFalse();
    }

    @Test
    public void someProxyIsAProxy() {
        when(proxySelector.select(URI)).thenReturn(Arrays.asList(A_PROXY));

        expect(proxyDetector.isProxyConfiguredFor(URI)).toBeTrue();
    }

    @Test
    public void mixedListIsAProxy() {
        when(proxySelector.select(URI)).thenReturn(Arrays.asList(A_PROXY, Proxy.NO_PROXY));

        expect(proxyDetector.isProxyConfiguredFor(URI)).toBeTrue();
    }

    @Test
    public void noListIsNotAProxy() {
        when(proxySelector.select(URI)).thenReturn(Collections.<Proxy>emptyList());

        expect(proxyDetector.isProxyConfiguredFor(URI)).toBeFalse();
    }

    @Test
    public void multipleNoProxiesAreStillNotAProxy() {
        when(proxySelector.select(URI)).thenReturn(Arrays.asList(Proxy.NO_PROXY, Proxy.NO_PROXY, Proxy.NO_PROXY));

        expect(proxyDetector.isProxyConfiguredFor(URI)).toBeFalse();
    }

    @Test
    public void handlesUrisWithNoSchema() throws URISyntaxException {
        final java.net.URI schemelessUri = new URI("/relative/path");
        when(proxySelector.select(schemelessUri)).thenThrow(new IllegalArgumentException("scheme == null"));

        expect(proxyDetector.isProxyConfiguredFor(schemelessUri)).toBeFalse();
    }

}
