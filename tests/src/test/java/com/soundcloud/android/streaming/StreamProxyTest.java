package com.soundcloud.android.streaming;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import rx.Observable;
import rx.Observer;

import android.net.Uri;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.regex.Pattern;

@RunWith(DefaultTestRunner.class)
public class StreamProxyTest {
    private StreamProxy subject;

    @Before public void before() {
        subject = new StreamProxy(Robolectric.application);
    }

    @Test
    public void testProxyLifeCycle() throws Exception {
        expect(subject.isRunning()).toBeFalse();

        subject.start();
        int tries = 5;
        while (!subject.isRunning() && tries-- > 0) {
            Thread.sleep(100);
        }
        expect(subject.isRunning()).toBeTrue();
        subject.stop();
        subject.join();
        expect(subject.isRunning()).toBeFalse();
    }

    @Test(expected = IllegalStateException.class)
    public void testProxyThrowsErrorIfStoppedAndNotStarted() throws Exception {
        subject.stop();
    }

    @Test
    public void testCreateHeader() throws Exception {
        Map<String, String> h = subject.headerMap();

        expect(h.containsKey("Server")).toBeTrue();
        expect(h.containsKey("Content-Type")).toBeTrue();
        expect(h.get("Content-Type")).toEqual("audio/mpeg");
    }

    @Test
    public void testReadRequest() throws Exception {
        HttpUriRequest req = StreamProxy.readRequest(getClass().getResourceAsStream("valid_request.txt"));
        expect(req.getFirstHeader("A-Header").getValue()).toEqual("Some-Value");
        expect(req.getURI().toString()).toEqual("foo");
        expect(req.getFirstHeader("Host")).toBeNull();
    }

    @Test
    public void testMalformedRequest() throws Exception {
        HttpUriRequest req = StreamProxy.readRequest(getClass().getResourceAsStream("malformed_request.txt"));
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
    public void testUriObservableReturnsCorrectUri() throws Exception {
        final String url = "https://api.soundcloud.com/tracks/3232/stream";
        final String nextUrl = null;

        subject.start();
        Observable<Uri> observable = subject.uriObservable(url, nextUrl);
        Uri result = observable.toBlockingObservable().getIterator().next();

        expect(result.toString()).toMatch(
                Pattern.quote("http://127.0.0.1:") + "\\d+" +
                Pattern.quote("/%2F?streamUrl=https%3A%2F%2Fapi.soundcloud.com%2Ftracks%2F3232%2Fstream"));

        subject.stop();
    }

    @Test
    public void testUriObservableErrorsIfStreamUrlIsEmpty() throws Exception {
        Observable<Uri> observable = subject.uriObservable("", null);
        Observer<Uri> observer = mock(Observer.class);
        observable.subscribe(observer);

        ArgumentCaptor<Throwable> argumentCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(observer).onError(argumentCaptor.capture());
        verify(observer, never()).onCompleted();
        verify(observer, never()).onNext(any(Uri.class));
        expect(argumentCaptor.getValue()).toBeInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void shouldParseRangeRequest() throws Exception {
        expect(StreamProxy.firstRequestedByte("bytes=100-")).toEqual(100l);
        expect(StreamProxy.firstRequestedByte("bytes=200-300")).toEqual(200l);
        expect(StreamProxy.firstRequestedByte("bytes=200-300,301-400")).toEqual(200l);
        expect(StreamProxy.firstRequestedByte("bytes=-100")).toEqual(0l);
        expect(StreamProxy.firstRequestedByte(null)).toEqual(0l);
        expect(StreamProxy.firstRequestedByte("")).toEqual(0l);
        expect(StreamProxy.firstRequestedByte("blargh")).toEqual(0l);
    }
}
