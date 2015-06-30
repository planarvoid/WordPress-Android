package com.soundcloud.android.api.legacy;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.UnauthorisedRequestRegistry;
import com.soundcloud.android.api.oauth.OAuth;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;

import com.soundcloud.android.utils.BuildHelper;
import com.soundcloud.android.utils.DeviceHelper;
import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.AuthenticationHandler;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.RedirectHandler;
import org.apache.http.client.RequestDirector;
import org.apache.http.client.UserTokenHandler;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestExecutor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.Context;

import java.io.IOException;
import java.net.URI;


@RunWith(SoundCloudTestRunner.class)
public class ApiWrapperTest {
    private PublicApiWrapper api;
    private final static String TEST_CLIENT_ID = "testClientId";
    private final static String TEST_CLIENT_SECRET = "testClientSecret";
    final FakeHttpLayer layer = new FakeHttpLayer();

    @Mock private Context context;
    @Mock private OAuth oAuth;
    @Mock private AccountOperations accountOperations;
    @Mock private BuildHelper buildHelper;
    @Mock private ObjectMapper objectMapper;
    @Mock private ApplicationProperties applicationProperties;
    @Mock private UnauthorisedRequestRegistry unauthorisedRequestRegistry;
    @Mock private DeviceHelper deviceHelper;

    @Before
    public void setup() {
        when(accountOperations.getSoundCloudToken()).thenReturn(new Token("access", "refresh"));
        when(oAuth.getClientId()).thenReturn(TEST_CLIENT_ID);
        when(oAuth.getClientSecret()).thenReturn(TEST_CLIENT_SECRET);
        api = new PublicApiWrapper(context, objectMapper, oAuth, accountOperations, applicationProperties,
                unauthorisedRequestRegistry, deviceHelper) {
            @Override
            protected RequestDirector getRequestDirector(HttpRequestExecutor requestExec,
                                                         ClientConnectionManager conman,
                                                         ConnectionReuseStrategy reustrat,
                                                         ConnectionKeepAliveStrategy kastrat,
                                                         HttpRoutePlanner rouplan,
                                                         HttpProcessor httpProcessor,
                                                         HttpRequestRetryHandler retryHandler,
                                                         RedirectHandler redirectHandler,
                                                         AuthenticationHandler targetAuthHandler,
                                                         AuthenticationHandler proxyAuthHandler,
                                                         UserTokenHandler stateHandler,
                                                         HttpParams params) {
                return new RequestDirector() {
                    @Override
                    public HttpResponse execute(HttpHost target, HttpRequest request, HttpContext context)
                            throws HttpException, IOException {
                        return layer.emulateRequest(request);
                    }
                };
            }
        };
        layer.clearHttpResponseRules();
    }

    @Test(expected = IllegalArgumentException.class)
    public void loginShouldThrowIllegalArgumentException() throws Exception {
        api.login(null, null);
    }

    @Test
    public void clientCredentialsShouldDefaultToSignupScope() throws Exception {
        layer.addPendingHttpResponse(200, "{\n" +
                "  \"access_token\":  \"04u7h-4cc355-70k3n\",\n" +
                "  \"expires_in\":    3600,\n" +
                "  \"scope\":         \"signup\",\n" +
                "  \"refresh_token\": \"04u7h-r3fr35h-70k3n\"\n" +
                "}");

        Token t = api.clientCredentials();
        assertThat(t.getAccessToken(), equalTo("04u7h-4cc355-70k3n"));
        assertThat(t.getRefreshToken(), equalTo("04u7h-r3fr35h-70k3n"));
        assertThat(t.getScope(), equalTo("signup"));
        assertTrue(t.hasScope(Token.SCOPE_SIGNUP));
        assertThat(t.getExpiresAt(), is(greaterThan(0L)));
    }

    @Test(expected = com.soundcloud.android.api.legacy.InvalidTokenException.class)
    public void clientCredentialsShouldThrowIfScopeCanNotBeObtained() throws Exception {
        layer.addPendingHttpResponse(200, "{\n" +
                "  \"access_token\":  \"04u7h-4cc355-70k3n\",\n" +
                "  \"expires_in\":    3600,\n" +
                "  \"scope\":         \"loser\",\n" +
                "  \"refresh_token\": \"04u7h-r3fr35h-70k3n\"\n" +
                "}");
        api.clientCredentials("unlimitedammo");
    }

    @Test
    public void shouldGetTokensWhenLoggingIn() throws Exception {
        layer.addPendingHttpResponse(200, "{\n" +
                "  \"access_token\":  \"04u7h-4cc355-70k3n\",\n" +
                "  \"expires_in\":    3600,\n" +
                "  \"scope\":         \"*\",\n" +
                "  \"refresh_token\": \"04u7h-r3fr35h-70k3n\"\n" +
                "}");

        Token t = api.login("foo", "bar");

        assertThat(t.getAccessToken(), equalTo("04u7h-4cc355-70k3n"));
        assertThat(t.getRefreshToken(), equalTo("04u7h-r3fr35h-70k3n"));
        assertThat(t.getScope(), equalTo("*"));
        assertThat(t.getExpiresAt(), is(greaterThan(0L)));
    }

    @Test
    public void shouldGetTokensWhenLoggingInWithNonExpiringScope() throws Exception {
        layer.addPendingHttpResponse(200, "{\n" +
                "  \"access_token\":  \"04u7h-4cc355-70k3n\",\n" +
                "  \"scope\":         \"* non-expiring\"\n" +
                "}");

        Token t = api.login("foo", "bar");
        assertThat(t.getAccessToken(), equalTo("04u7h-4cc355-70k3n"));
        assertThat(t.getRefreshToken(), is(nullValue()));
        assertThat(t.getExpiresAt(), is(0L));
        assertThat(t.hasScope(Token.SCOPE_NON_EXPIRING), is(true));
        assertThat(t.hasScope(Token.SCOPE_DEFAULT), is(true));
    }

    @Test(expected = InvalidTokenException.class)
    public void shouldThrowInvalidTokenExceptionWhenLoginFailed() throws Exception {
        layer.addPendingHttpResponse(401, "{\n" +
                "  \"error\":  \"Error!\"\n" +
                "}");
        api.login("foo", "bar");
    }


    @Test(expected = com.soundcloud.android.api.legacy.ApiResponseException.class)
    public void shouldThrowApiResponseExceptionWhenInvalidJSONReturned() throws Exception {
        layer.addPendingHttpResponse(200, "I'm invalid JSON!");
        api.login("foo", "bar");
    }

    @Test
    public void shouldContainInvalidJSONInExceptionMessage() throws Exception {
        layer.addPendingHttpResponse(200, "I'm invalid JSON!");
        try {
            api.login("foo", "bar");
            fail("expected IOException");
        } catch (com.soundcloud.android.api.legacy.ApiResponseException e) {
            assertThat(e.getMessage(), containsString("I'm invalid JSON!"));
        }
    }

    @Test
    public void shouldRefreshToken() throws Exception {
        layer.addPendingHttpResponse(200, "{\n" +
                "  \"access_token\":  \"fr3sh\",\n" +
                "  \"expires_in\":    3600,\n" +
                "  \"scope\":         \"default\",\n" +
                "  \"refresh_token\": \"refresh\"\n" +
                "}");


        assertThat(api.refreshToken().getAccessToken(), equalTo("fr3sh"));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowIllegalStateExceptionWhenNoRefreshToken() throws Exception {
        when(accountOperations.getSoundCloudToken()).thenReturn(new Token("access", null));
        api.refreshToken();
    }

    @Test
    public void shouldResolveUris() throws Exception {
        HttpResponse r = mock(HttpResponse.class);
        StatusLine line = mock(StatusLine.class);
        when(line.getStatusCode()).thenReturn(302);
        when(r.getStatusLine()).thenReturn(line);
        Header location = mock(Header.class);
        when(location.getValue()).thenReturn("http://api.soundcloud.com/users/1000");
        when(r.getFirstHeader(anyString())).thenReturn(location);
        layer.addHttpResponseRule(new RequestMatcher() {
            @Override
            public boolean matches(HttpRequest request) {
                return true;
            }
        }, r);
        assertThat(api.resolve("http://soundcloud.com/crazybob"), is(1000L));
    }

    @Test(expected = com.soundcloud.android.api.legacy.ResolverException.class)
    public void resolveShouldReturnNegativeOneWhenInvalid() throws Exception {
        layer.addPendingHttpResponse(404, "Not found");
        api.resolve("http://soundcloud.com/nonexisto");
    }

    @Test
    public void shouldGetContent() throws Exception {
        layer.addHttpResponseRule("/some/resource?a=1&client_id=" + TEST_CLIENT_ID, "response");
        assertThat(com.soundcloud.android.api.legacy.Http.getString(api.get(com.soundcloud.android.api.legacy.Request.to("/some/resource").with("a", "1"))),
                equalTo("response"));
    }

    @Test
    public void shouldPostContent() throws Exception {
        HttpResponse resp = mock(HttpResponse.class);
        layer.addHttpResponseRule("POST", "/foo/something", resp);
        assertThat(api.post(com.soundcloud.android.api.legacy.Request.to("/foo/something").with("a", 1)),
                equalTo(resp));
    }

    @Test
    public void shouldPutContent() throws Exception {
        HttpResponse resp = mock(HttpResponse.class);
        layer.addHttpResponseRule("PUT", "/foo/something", resp);
        assertThat(api.put(com.soundcloud.android.api.legacy.Request.to("/foo/something").with("a", 1)),
                equalTo(resp));
    }

    @Test
    public void shouldDeleteContent() throws Exception {
        HttpResponse resp = mock(HttpResponse.class);
        layer.addHttpResponseRule("DELETE", "/foo/something?client_id=" + TEST_CLIENT_ID, resp);
        assertThat(api.delete(new com.soundcloud.android.api.legacy.Request("/foo/something")), equalTo(resp));
    }

    @Test
    public void testGetOAuthHeader() throws Exception {
        Header h = OAuth.createOAuthHeader(new Token("foo", "refresh"));
        assertThat(h.getName(), equalTo("Authorization"));
        assertThat(h.getValue(), equalTo("OAuth foo"));
    }

    @Test
    public void testGetOAuthHeaderNullToken() throws Exception {
        Header h = OAuth.createOAuthHeader(null);
        assertThat(h.getName(), equalTo("Authorization"));
        assertThat(h.getValue(), equalTo("OAuth invalidated"));
    }

    @Test
    public void shouldCallTokenStateListenerWhenTokenIsInvalidated() throws Exception {
        com.soundcloud.android.api.legacy.TokenListener listener = mock(com.soundcloud.android.api.legacy.TokenListener.class);
        api.setTokenListener(listener);
        final Token old = api.getToken();
        api.invalidateToken();
        verify(listener).onTokenInvalid(old);
    }

    @Test
    public void invalidateTokenShouldTryToGetAlternativeToken() throws Exception {
        com.soundcloud.android.api.legacy.TokenListener listener = mock(com.soundcloud.android.api.legacy.TokenListener.class);
        final Token cachedToken = new Token("new", "fresh");
        api.setTokenListener(listener);
        when(listener.onTokenInvalid(api.getToken())).thenReturn(cachedToken);
        assertThat(api.invalidateToken(), equalTo(cachedToken));
    }

    @Test
    public void invalidateTokenShouldReturnNullIfNoListenerAvailable() throws Exception {
        assertThat(api.invalidateToken(), is(nullValue()));
    }

    @Test
    public void shouldCallTokenStateListenerWhenTokenIsRefreshed() throws Exception {
        layer.addPendingHttpResponse(200, "{\n" +
                "  \"access_token\":  \"fr3sh\",\n" +
                "  \"expires_in\":    3600,\n" +
                "  \"scope\":         \"default\",\n" +
                "  \"refresh_token\": \"refresh\"\n" +
                "}");

        com.soundcloud.android.api.legacy.TokenListener listener = mock(TokenListener.class);

        api.setTokenListener(listener);
        api.refreshToken();
        verify(listener).onTokenRefreshed(new Token("fr3sh", "refresh", "default", 3600L));
    }

    @Test
    public void testShouldAlwaysAddClientIdEvenWhenAuthenticated() throws Exception {
        layer.addHttpResponseRule("/foo?client_id=" + TEST_CLIENT_ID, "body");
        final com.soundcloud.android.api.legacy.Request request = com.soundcloud.android.api.legacy.Request.to("/foo");
        final HttpResponse response = api.get(request);
        assertEquals("body", com.soundcloud.android.api.legacy.Http.getString(response));
    }

    @Test
    public void testDontAddClientIdIfManuallyAdded() throws Exception {
        layer.addHttpResponseRule("/foo?client_id=12345", "body");
        final com.soundcloud.android.api.legacy.Request req = com.soundcloud.android.api.legacy.Request.to("/foo").with("client_id", "12345");
        final HttpResponse response = api.get(req);
        assertEquals("body", com.soundcloud.android.api.legacy.Http.getString(response));
    }

    @Test
    public void shouldSetProxy() throws Exception {
        assertFalse(api.isProxySet());
        URI proxy = URI.create("https://foo.com");
        assertEquals(proxy.getPort(), -1);
        api.setProxy(proxy);
        assertTrue(api.isProxySet());
        assertEquals("https://foo.com:443", api.getProxy().toString());


        api.setProxy(URI.create("https://foo.com:12345"));
        assertEquals(URI.create("https://foo.com:12345"), api.getProxy());
    }

    @Test
    @SuppressWarnings("serial")
    public void shouldHandleBrokenHttpClientNPE() throws Exception {
        final HttpClient client = mock(HttpClient.class);
        PublicApiWrapper broken = new PublicApiWrapper(context, objectMapper, oAuth, accountOperations, applicationProperties,
                unauthorisedRequestRegistry, deviceHelper) {
            @Override
            public HttpClient getHttpClient() {
                return client;
            }

            @Override
            public long resolve(String url) throws IOException {
                HttpResponse resp = get(com.soundcloud.android.api.legacy.Request.to(Endpoints.RESOLVE).with("url", url));
                if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY) {
                    Header location = resp.getFirstHeader("Location");
                    if (location != null && location.getValue() != null) {
                        final String path = URI.create(location.getValue()).getPath();
                        if (path != null && path.contains("/")) {
                            try {
                                final String id = path.substring(path.lastIndexOf('/') + 1);
                                return Integer.parseInt(id);
                            } catch (NumberFormatException e) {
                                throw new com.soundcloud.android.api.legacy.ResolverException(e, resp);
                            }
                        } else {
                            throw new com.soundcloud.android.api.legacy.ResolverException("Invalid string:" + path, resp);
                        }
                    } else {
                        throw new com.soundcloud.android.api.legacy.ResolverException("No location header", resp);
                    }
                } else {
                    throw new ResolverException("Invalid status code", resp);
                }
            }

            @Override
            public HttpResponse get(com.soundcloud.android.api.legacy.Request request) throws IOException {
                return execute(request, HttpGet.class);
            }

            @Override
            public HttpResponse put(com.soundcloud.android.api.legacy.Request request) throws IOException {
                return execute(request, HttpPut.class);
            }

            @Override
            public HttpResponse post(com.soundcloud.android.api.legacy.Request request) throws IOException {
                return execute(request, HttpPost.class);
            }

            @Override
            public HttpResponse delete(com.soundcloud.android.api.legacy.Request request) throws IOException {
                return execute(request, HttpDelete.class);
            }

            // This design existed when the library was brought in to the project. Changing
            // it does not seem worthwhile, since the reassignment is once during validation
            // of inputs.
            @SuppressWarnings("PMD.AvoidReassigningParameters")
            protected HttpResponse execute(com.soundcloud.android.api.legacy.Request req, Class<? extends HttpRequestBase> reqType) throws IOException {
                com.soundcloud.android.api.legacy.Request defaults = PublicApiWrapper.defaultParams.get();
                if (defaults != null && !defaults.getParams().isEmpty()) {
                    // copy + merge in default parameters
                    req = new com.soundcloud.android.api.legacy.Request(req);
                    for (NameValuePair nvp : defaults) {
                        req.add(nvp.getName(), nvp.getValue());
                    }
                }
                logRequest(reqType, req);
                if (!req.getParams().containsKey(OAuth.PARAM_CLIENT_ID)) {
                    req = new com.soundcloud.android.api.legacy.Request(req).add(OAuth.PARAM_CLIENT_ID, oAuth.getClientId());
                }
                return execute(req.buildRequest(reqType));
            }
        };
        when(client.execute(any(HttpHost.class), any(HttpUriRequest.class))).thenThrow(new NullPointerException());
        try {
            broken.execute(new HttpGet("/foo"));
            fail("expected BrokenHttpClientException");
        } catch (com.soundcloud.android.api.legacy.BrokenHttpClientException expected) {
            // make sure client retried request
            verify(client, times(2)).execute(any(HttpHost.class), any(HttpUriRequest.class));
        }
    }

    @Test
    @SuppressWarnings("serial")
    public void shouldHandleBrokenHttpClientIAE() throws Exception {
        final HttpClient client = mock(HttpClient.class);
        PublicApiWrapper broken = new PublicApiWrapper(context, objectMapper, oAuth, accountOperations, applicationProperties,
                unauthorisedRequestRegistry, deviceHelper) {
            @Override
            public HttpClient getHttpClient() {
                return client;
            }
        };
        when(client.execute(any(HttpHost.class), any(HttpUriRequest.class))).thenThrow(new IllegalArgumentException());
        try {
            broken.execute(new HttpGet("/foo"));
            fail("expected BrokenHttpClientException");
        } catch (com.soundcloud.android.api.legacy.BrokenHttpClientException expected) {
            verify(client, times(1)).execute(any(HttpHost.class), any(HttpUriRequest.class));
        }
    }

    @SuppressWarnings("serial")
    @Test
    public void shouldSafeExecute() throws Exception {

        final HttpClient client = mock(HttpClient.class);
        PublicApiWrapper broken = new PublicApiWrapper(context, objectMapper, oAuth, accountOperations, applicationProperties,
                unauthorisedRequestRegistry, deviceHelper) {
            @Override
            public HttpClient getHttpClient() {
                return client;
            }
        };
        when(client.execute(any(HttpHost.class), any(HttpUriRequest.class))).thenThrow(new IllegalArgumentException());
        try {
            HttpGet request = new HttpGet("/foo");
            assertNotNull(request);
            assertNotNull(broken);
            broken.safeExecute(null, request);
            fail("expected NullPointerException");
        } catch (NullPointerException expected) {
            // expected
        }

        reset(client);
        when(client.execute(any(HttpHost.class), any(HttpUriRequest.class))).thenThrow(new NullPointerException());
        try {
            broken.execute(new HttpGet("/foo"));
            fail("expected BrokenHttpClientException");
        } catch (BrokenHttpClientException expected) {
            // make sure client retried request
            verify(client, times(2)).execute(any(HttpHost.class), any(HttpUriRequest.class));
        }
    }

    @Test
    public void testAddDefaultParameters() throws Exception {
        layer.addHttpResponseRule("/foo?client_id=" + TEST_CLIENT_ID, "Hi");
        layer.addHttpResponseRule("/foo?t=1&client_id=" + TEST_CLIENT_ID, "Hi t1");
        layer.addHttpResponseRule("/foo?t=2&client_id=" + TEST_CLIENT_ID, "Hi t2");

        final com.soundcloud.android.api.legacy.Request foo = Request.to("/foo");
        for (int i = 0; i < 1000; i++) {
            final Exception throwable[] = new Exception[2];
            Thread t1 = new Thread("t1") {
                @Override
                public void run() {
                    PublicApiWrapper.setDefaultParameter("t", "1");
                    try {
                        assertEquals("Hi t1", com.soundcloud.android.api.legacy.Http.getString(api.get(foo)));
                    } catch (Exception e) {
                        throwable[0] = e;
                    }
                    PublicApiWrapper.clearDefaultParameters();
                    try {
                        assertEquals("Hi", com.soundcloud.android.api.legacy.Http.getString(api.get(foo)));
                    } catch (Exception e) {
                        throwable[0] = e;
                    }
                }
            };

            Thread t2 = new Thread("t2") {
                @Override
                public void run() {
                    PublicApiWrapper.setDefaultParameter("t", "2");
                    try {
                        assertEquals("Hi t2", com.soundcloud.android.api.legacy.Http.getString(api.get(foo)));
                    } catch (Exception e) {
                        throwable[1] = e;
                    }
                    PublicApiWrapper.clearDefaultParameters();
                    try {
                        assertEquals("Hi", com.soundcloud.android.api.legacy.Http.getString(api.get(foo)));
                    } catch (Exception e) {
                        throwable[1] = e;
                    }
                }
            };
            t1.start();
            t2.start();
            t1.join();
            t2.join();
            if (throwable[0] != null) {
                throw throwable[0];
            }
            if (throwable[1] != null) {
                throw throwable[1];
            }

            assertEquals("Hi", Http.getString(api.get(foo)));
        }
    }
}
