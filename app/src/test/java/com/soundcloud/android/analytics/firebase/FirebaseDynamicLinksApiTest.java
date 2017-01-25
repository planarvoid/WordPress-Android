package com.soundcloud.android.analytics.firebase;

import static org.mockito.Mockito.when;

import com.google.firebase.FirebaseOptions;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.strings.Charsets;
import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.http.RealResponseBody;
import okio.Buffer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import rx.Scheduler;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import android.content.res.Resources;

import java.io.IOException;

public class FirebaseDynamicLinksApiTest extends AndroidUnitTest {
    private static final FirebaseOptions OPTIONS = new FirebaseOptions.Builder().setApplicationId("id").setApiKey("key").build();

    @Mock private Resources resources;
    @Mock private OkHttpClient httpClient;
    @Mock private Call call;
    private Scheduler scheduler = Schedulers.immediate();
    private TestSubscriber<String> subscriber = new TestSubscriber<>();
    private FirebaseDynamicLinksApi api;

    @Before
    public void setUp() throws Exception {
        api = new FirebaseDynamicLinksApi(OPTIONS, resources, httpClient, scheduler);
    }

    @Test
    public void createDynamicLinkEmitsResultFromFirebase() throws IOException {
        ArgumentCaptor<Request> request = ArgumentCaptor.forClass(Request.class);
        when(httpClient.newCall(request.capture())).thenReturn(call);
        when(call.execute()).thenAnswer(invocation -> new Response.Builder()
                .protocol(Protocol.HTTP_2)
                .request(request.getValue())
                .code(200)
                .body(body("{\"shortLink\":\"http://bar\"}"))
                .build());
        api.createDynamicLink("http://foo").subscribe(subscriber);
        subscriber.assertValue("http://bar");
    }

    @Test
    public void createDynamicLinkEmitsIoExceptionWhenResponseIsUnsuccessful() throws IOException {
        ArgumentCaptor<Request> request = ArgumentCaptor.forClass(Request.class);
        when(httpClient.newCall(request.capture())).thenReturn(call);
        when(call.execute()).thenAnswer(invocation -> new Response.Builder()
                .protocol(Protocol.HTTP_2)
                .request(request.getValue())
                .code(500)
                .body(body("{}"))
                .build());
        TestSubscriber<String> subscriber = new TestSubscriber<>();
        api.createDynamicLink("http://foo").subscribe(subscriber);
        subscriber.assertError(IOException.class);
    }

    private RealResponseBody body(String json) {
        return new RealResponseBody(Headers.of(), new Buffer().writeString(json, Charsets.UTF_8));
    }
}
