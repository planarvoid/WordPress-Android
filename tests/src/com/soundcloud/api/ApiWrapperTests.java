package com.soundcloud.api;

import static junit.framework.Assert.fail;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.tester.org.apache.http.RequestMatcher;
import junit.framework.Assert;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;


@RunWith(DefaultTestRunner.class)
public class ApiWrapperTests {
    private ApiWrapper api;

    @Before
    public void setup() {
        api = new ApiWrapper("invalid", "invalid", null, null, CloudAPI.Env.SANDBOX);
    }

    @Test(expected = IllegalArgumentException.class)
    public void loginShouldThrowIllegalArgumentException() throws Exception {
        api.login(null, null);
    }

    @Test
    public void shouldGetTokensWhenLoggingIn() throws Exception {
        Robolectric.addPendingHttpResponse(200, "{\n" +
                "  \"access_token\":  \"04u7h-4cc355-70k3n\",\n" +
                "  \"expires_in\":    3600,\n" +
                "  \"scope\":         null,\n" +
                "  \"refresh_token\": \"04u7h-r3fr35h-70k3n\"\n" +
                "}");

        api.login("foo", "bar");
        assertThat(api.getToken(), equalTo("04u7h-4cc355-70k3n"));
        assertThat(api.getRefreshToken(), equalTo("04u7h-r3fr35h-70k3n"));
        assertNull(api.getScope());
        assertNotNull(api.getExpiresIn());
    }

    @Test(expected = IOException.class)
    public void shouldThrowIOExceptionWhenLoginFailed() throws Exception {
        Robolectric.addPendingHttpResponse(401, "{\n" +
                "  \"error\":  \"Error!\"\n" +
                "}");
        api.login("foo", "bar");
    }


    @Test(expected = IOException.class)
    public void shouldThrowIOExceptonWhenInvalidJSONReturned() throws Exception {
        Robolectric.addPendingHttpResponse(200, "I'm invalid JSON!");
        api.login("foo", "bar");
    }

    @Test
    public void shouldContainInvalidJSONInExceptionMessage() throws Exception {
        Robolectric.addPendingHttpResponse(200, "I'm invalid JSON!");
        try {
            api.login("foo", "bar");
            fail("expected IOException");
        } catch (IOException e) {
            assertThat(e.getMessage(), containsString("I'm invalid JSON!"));
        }
    }

    @Test
    public void shouldRefreshToken() throws Exception {
        Robolectric.addPendingHttpResponse(200, "{\n" +
                "  \"access_token\":  \"fr3sh\",\n" +
                "  \"expires_in\":    3600,\n" +
                "  \"scope\":         null,\n" +
                "  \"refresh_token\": \"refresh\"\n" +
                "}");

        assertThat(new ApiWrapper("1234", "5678", null, "sofreshexciting", CloudAPI.Env.SANDBOX)
                .refreshToken()
                .getToken(),
                equalTo("fr3sh"));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowIllegalStateExceptionWhenNoRefreshToken() throws Exception {
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
        Robolectric.addHttpResponseRule(new RequestMatcher() {
            @Override
            public boolean matches(HttpRequest request) {
                return true;
            }
        }, r);
        assertThat(api.resolve("http://soundcloud.com/crazybob"), is(1000L));
    }

    @Test
    public void resolveShouldReturnNegativeOneWhenInvalid() throws Exception {
        Robolectric.addPendingHttpResponse(404, "Not found");
        assertThat(api.resolve("http://soundcloud.com/nonexisto"), equalTo(-1L));
    }

    @Test
    public void unauthorizeShouldResetTokenInformation() throws Exception {
        api.updateTokens(null, null);
        assertNull(api.getRefreshToken());
        assertNull(api.getToken());
        assertNull(api.getScope());
        assertNull(api.getExpiresIn());
    }


    @Test
    public void shouldGetContent() throws Exception {
        Robolectric.addHttpResponseRule("/some/resource?a=1", "response");
        assertThat(Http.getString(api.getContent("/some/resource", new Http.Params("a", "1"))),
                equalTo("response"));
    }

    @Test
    public void shouldPostContent() throws Exception {
        HttpResponse resp = mock(HttpResponse.class);
        Robolectric.addHttpResponseRule("POST", "/foo/something?a=1", resp);
        assertThat(api.postContent("/foo/something", new Http.Params("a", 1)),
                equalTo(resp));
    }

    @Test
    public void shouldPutContent() throws Exception {
        HttpResponse resp = mock(HttpResponse.class);
        Robolectric.addHttpResponseRule("PUT", "/foo/something?a=1", resp);
        assertThat(api.putContent("/foo/something", new Http.Params("a", 1)),
                equalTo(resp));
    }

    @Test
    public void shouldDeleteContent() throws Exception {
        HttpResponse resp = mock(HttpResponse.class);
        Robolectric.addHttpResponseRule("DELETE", "/foo/something", resp);
        assertThat(api.deleteContent("/foo/something"), equalTo(resp));
    }

    @Test
    public void testGetOAuthHeader() throws Exception {
        Header h = ApiWrapper.getOAuthHeader("foo");
        assertThat(h.getName(), equalTo("Authorization"));
        assertThat(h.getValue(), equalTo("OAuth foo"));
    }

    @Test
    public void testGetOAuthHeaderNullToken() throws Exception {
        Header h = ApiWrapper.getOAuthHeader(null);
        assertThat(h.getName(), equalTo("Authorization"));
        assertThat(h.getValue(), equalTo("OAuth invalidated"));
    }
}
