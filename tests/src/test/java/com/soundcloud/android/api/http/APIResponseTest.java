package com.soundcloud.android.api.http;

import static com.soundcloud.android.Expect.expect;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.junit.Before;
import org.junit.Test;

public class APIResponseTest {

    private final Header[] responseHeaders = new Header[]{new BasicHeader("key", "value")};
    private APIResponse response;

    @Before
    public void setUp() {
        response = new APIResponse(200,"response", responseHeaders);
    }

    @Test
    public void shouldReturnTrueForSuccessResponse() {
        expect(response.isSuccess()).toEqual(true);
    }

    @Test
    public void shouldReturnFalseFor400Response() {
        expect(new APIResponse(404,"response", responseHeaders).isNotSuccess()).toEqual(true);
    }

    @Test
    public void shouldReturnFalseFor199Response() {
        expect(new APIResponse(199,"response", responseHeaders).isNotSuccess()).toEqual(true);
    }

    @Test
    public void shouldReturnTrueForRateLimitedResponse() {
        expect(new APIResponse(429,"response", responseHeaders).accountIsRateLimited()).toEqual(true);
    }

    @Test
    public void shouldReturnFalseForFailedResponse() {
        expect(response.getHeader("key")).toEqual("value");
    }

    @Test
    public void shouldReturnTrueIfResponseHasBody() {
        expect(response.hasResponseBody()).toEqual(true);
    }

    @Test
    public void shouldReturnFalseForRateLimitedResponse() {
        expect(new APIResponse(429,"  ", responseHeaders).hasResponseBody()).toEqual(false);
    }

    @Test
    public void shouldReturnSpecifiedResponseBody() {
        expect(response.getResponseBody()).toEqual("response");
    }
}
