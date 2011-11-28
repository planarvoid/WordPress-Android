package com.soundcloud.android.streaming;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.acra.util.HttpRequest;
import org.apache.http.client.methods.HttpGet;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

@RunWith(DefaultTestRunner.class)
public class StreamProxyTest {

    @Test
    @Ignore
    public void testStartProxy() throws Exception {
        StreamProxy proxy = new StreamProxy(DefaultTestRunner.application, 0);
        proxy.loader.setForceOnline(true);
        proxy.init()
                .start()
                .join();
    }

    @Test
    public void testCreateHeader() throws Exception {
        StreamProxy proxy = new StreamProxy(DefaultTestRunner.application, 0);

        Map<String, String> h = proxy.headerMap();

        expect(h.containsKey("Server")).toBeTrue();
        expect(h.containsKey("Content-Type")).toBeTrue();
        expect(h.get("Content-Type")).toEqual("audio/mpeg");
    }


    @Test
    public void testReadRequest() throws Exception {
        HttpGet req = StreamProxy.readRequest(getClass().getResourceAsStream("valid_request.txt"));
        expect(req.getFirstHeader("A-Header").getValue()).toEqual("Some-Value");
        expect(req.getURI().toString()).toEqual("foo");
        expect(req.getFirstHeader("Host")).toBeNull();
    }

    @Test
    public void testMalformedRequest() throws Exception {
        HttpGet req = StreamProxy.readRequest(getClass().getResourceAsStream("malformed_request.txt"));
        expect(req.getURI().toString()).toEqual("foo");
    }

    @Test(expected = IOException.class)
    public void shouldOnlySupportGETRequests() throws Exception {
        StreamProxy.readRequest(new ByteArrayInputStream("POST /foo HTTP/1.1".getBytes()));
    }

    @Test(expected = IOException.class)
    public void shouldThrowIOWhenInputIsEmpty() throws Exception {
        StreamProxy.readRequest(new ByteArrayInputStream("".getBytes()));
    }

    @Test(expected = URISyntaxException.class)
    public void shouldHandleInvalidUris() throws Exception {
       StreamProxy.readRequest(new ByteArrayInputStream("GET /dasdas\u0005?? HTTP/1.1".getBytes()));
    }

    @Test
    public void testCreateUri() throws Exception {
        StreamProxy proxy = new StreamProxy(DefaultTestRunner.application, 0);
        expect(proxy.createUri("https://api.soundcloud.com/tracks/3232/stream", null).toString())
                .toEqual("http://127.0.0.1:0/%2F?streamUrl=https%3A%2F%2Fapi.soundcloud.com%2Ftracks%2F3232%2Fstream");
    }
}
