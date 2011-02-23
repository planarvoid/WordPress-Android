
package com.soundcloud.utils.http;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;

public class CountingMultipartRequestEntity implements HttpEntity {
    private HttpEntity delegate_;

    private Http.ProgressListener listener_;

    public CountingMultipartRequestEntity(HttpEntity delegate, Http.ProgressListener listener) {
        super();
        delegate_ = delegate;
        listener_ = listener;
    }

    public void consumeContent() throws IOException {
        delegate_.consumeContent();
    }

    public InputStream getContent() throws IOException, IllegalStateException {
        return delegate_.getContent();
    }

    public Header getContentEncoding() {
        return delegate_.getContentEncoding();
    }

    public long getContentLength() {
        return delegate_.getContentLength();
    }

    public Header getContentType() {
        return delegate_.getContentType();
    }

    public boolean isChunked() {
        return delegate_.isChunked();
    }

    public boolean isRepeatable() {
        return delegate_.isRepeatable();
    }

    public boolean isStreaming() {
        return delegate_.isStreaming();
    }

    public void writeTo(OutputStream outstream) throws IOException {
        delegate_.writeTo(new CountingOutputStream(outstream, listener_));
    }

    private class CountingOutputStream extends FilterOutputStream {

        private final Http.ProgressListener listener;

        private long transferred;

        public CountingOutputStream(final OutputStream out, final Http.ProgressListener listener) {
            super(out);
            this.listener = listener;
            this.transferred = 0;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len);
            this.transferred += len;
            if (listener != null)
                this.listener.transferred(this.transferred);
        }

        @Override
        public void write(int b) throws IOException {
            out.write(b);
            this.transferred++;
            if (listener != null)
                this.listener.transferred(this.transferred);
        }
    }
}
