package com.soundcloud.android.api;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.api.ApiRequest.ProgressListener;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;
import okio.BufferedSink;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;

public class ProgressRequestBodyTest {

    private static final String CONTENT = "hello!";

    @Mock private BufferedSink sourceSink;
    @Mock private ProgressListener progressListener;
    @Captor private ArgumentCaptor<BufferedSink> sinkCaptor;

    private ProgressRequestBody requestBody;
    private RequestBody wrappedBody = RequestBody.create(MediaType.parse("text/html"), CONTENT);

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        requestBody = new ProgressRequestBody(wrappedBody, progressListener);
    }

    @Test
    public void shouldForwardProgressToListener() throws IOException {
        requestBody.writeTo(sourceSink);

        verify(progressListener).update(CONTENT.length(), CONTENT.length());
    }

    @Test
    public void shouldFlushTheSink() throws IOException {
        requestBody.writeTo(sourceSink);

        verify(sourceSink).flush();
    }

}