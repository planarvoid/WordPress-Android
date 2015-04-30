package com.soundcloud.android.api;

import com.soundcloud.android.api.ApiRequest.ProgressListener;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;

import java.io.IOException;

class ProgressRequestBody extends RequestBody {

    private final RequestBody requestBody;
    private final ProgressListener progressListener;

    public ProgressRequestBody(RequestBody requestBody, ProgressListener progressListener) {
        this.requestBody = requestBody;
        this.progressListener = progressListener;
    }

    @Override
    public MediaType contentType() {
        return requestBody.contentType();
    }

    @Override
    public long contentLength() throws IOException {
        return requestBody.contentLength();
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        final long totalBytes = contentLength();
        final BufferedSink progressSink = Okio.buffer(new ForwardingSink(sink) {
            private long bytesWritten;

            @Override
            public void write(Buffer source, long byteCount) throws IOException {
                bytesWritten += byteCount;
                progressListener.update(bytesWritten, totalBytes);
                super.write(source, byteCount);
            }
        });
        requestBody.writeTo(progressSink);
        progressSink.flush();
    }
}