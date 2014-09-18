package com.soundcloud.api;

import org.apache.http.Header;
import org.apache.http.HttpEntity;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class CountingMultipartEntity implements HttpEntity {
    private HttpEntity delegate;
    private Request.TransferProgressListener listener;

    public CountingMultipartEntity(HttpEntity delegate,
                                   Request.TransferProgressListener listener) {
        super();
        this.delegate = delegate;
        this.listener = listener;
    }

    public void consumeContent() throws IOException {
        delegate.consumeContent();
    }

    public InputStream getContent() throws IOException, IllegalStateException {
        return delegate.getContent();
    }

    public Header getContentEncoding() {
        return delegate.getContentEncoding();
    }

    public long getContentLength() {
        return delegate.getContentLength();
    }

    public Header getContentType() {
        return delegate.getContentType();
    }

    public boolean isChunked() {
        return delegate.isChunked();
    }

    public boolean isRepeatable() {
        return delegate.isRepeatable();
    }

    public boolean isStreaming() {
        return delegate.isStreaming();
    }

    public void writeTo(OutputStream outstream) throws IOException {
        delegate.writeTo(new CountingOutputStream(outstream, listener));
    }

    private static class CountingOutputStream extends FilterOutputStream {
        private final Request.TransferProgressListener listener;
        private long transferred = 0;

        public CountingOutputStream(final OutputStream out, final Request.TransferProgressListener listener) {
            super(out);
            this.listener = listener;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len);
            transferred += len;
            if (listener != null) {
                listener.transferred(transferred);
            }
        }

        @Override
        public void write(int b) throws IOException {
            out.write(b);
            transferred++;
            if (listener != null) {
                listener.transferred(transferred);
            }
        }
    }
}
