package com.soundcloud.android.api.legacy;

import static com.soundcloud.android.Expect.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.http.HttpHost;
import org.junit.Test;

import java.net.URI;


public class EnvTest {
    @Test
    public void testIsApiHost() throws Exception {
        assertTrue(Env.LIVE.isApiHost(new HttpHost("api.soundcloud.com", 80, "http")));
        assertTrue(Env.LIVE.isApiHost(new HttpHost("api.soundcloud.com", 443, "https")));
        assertFalse(Env.LIVE.isApiHost(new HttpHost("foo.soundcloud.com", 443, "https")));
    }

    @Test
    public void shouldHostsShouldExplicitlySpecifyPorts() throws Exception {
        assertEquals(443, Env.LIVE.getSecureAuthResourceHost().getPort());
        assertEquals(443, Env.LIVE.getSecureResourceHost().getPort());
    }

    @Test
    public void uriUsesHttpsAndBareDomain() {
        expect(Env.LIVE.getSecureAuthResourceURI()).toEqual(URI.create("https://soundcloud.com"));
    }
}
