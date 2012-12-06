/*-
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.soundcloud.android.imageloader;

import static com.soundcloud.android.imageloader.ImageLoader.TAG;

import com.soundcloud.android.utils.IOUtils;
import org.jetbrains.annotations.NotNull;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Log;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ContentHandler;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * A {@link ContentHandler} that decodes a {@link Bitmap} from a {@link URLConnection}.
 * <p>
 * The implementation includes a work-around for <a
 * href="http://code.google.com/p/android/issues/detail?id=6066">Issue 6066</a>.
 * <p>
 * An {@link IOException} is thrown if there is a decoding exception.
 */
public class DownloadBitmapHandler extends ContentHandler {
    public static final int READ_TIMEOUT    = 10000;
    public static final int CONNECT_TIMEOUT = 3000;
    private static final int LOADTIME_WARN  = 10 * 1000; // flag requests taking longer than 10 sec

    private static final int MAX_REDIRECTS = 1;
    private final boolean mUseCache;

    public DownloadBitmapHandler() {
        this(true);
    }

    public DownloadBitmapHandler(boolean usecache) {
        mUseCache = usecache;
    }

    @Override @NotNull
    public Bitmap getContent(URLConnection connection) throws IOException {
        return doGetContent((HttpURLConnection) connection, 0);
    }

    @NotNull
    private Bitmap doGetContent(HttpURLConnection connection, int redirects) throws IOException {
        final long start = System.currentTimeMillis();
        final URL url = connection.getURL();

        connection.setRequestProperty("Accept-Encoding", "identity");
        connection.setRequestProperty("Connection", "Close");
        connection.setReadTimeout(READ_TIMEOUT);
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setUseCaches(mUseCache);
        connection.setInstanceFollowRedirects(false);

        try {
            final int code = connection.getResponseCode();
            switch (code) {
                case 302:
                    if (redirects < MAX_REDIRECTS) {
                        String location = connection.getHeaderField("Location");
                        if (!TextUtils.isEmpty(location)) {
                            return doGetContent((HttpURLConnection) new URL(location).openConnection(), redirects+1);
                        } else {
                            throw new IOException("redirect without location header");
                        }
                    } else {
                        throw new IOException("Reached max redirects: " +redirects);
                    }

                case 200:
                    Log.d(TAG, "loaded " + IOUtils.md5(url.toString()));

                    InputStream input = new BlockingFilterInputStream(connection.getInputStream());
                    Bitmap bitmap = BitmapFactory.decodeStream(input);
                    if (bitmap == null) {
                        throw new IOException("Image could not be decoded");
                    } else {
                        return bitmap;
                    }
                default:
                    throw new IOException("response code "+code+ " received");
            }
        } finally {
            final long loadTime = System.currentTimeMillis() - start;
            if (loadTime > LOADTIME_WARN) {
                Log.w(TAG, "slow image loading request " + loadTime + " ms, url = " + url);
            } else if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "image fetched in " + loadTime + " ms, url = " + url);
            }
            // should only be needed for Keep-Alive which we're not using
            connection.disconnect();
        }
    }

    /**
     * A {@link java.io.FilterInputStream} that blocks until the requested number of bytes
     * have been read/skipped, or the end of the stream is reached.
     * <p>
     * This filter can be used as a work-around for <a
     * href="http://code.google.com/p/android/issues/detail?id=6066">Issue
     * #6066</a>.
     */
    static class BlockingFilterInputStream extends FilterInputStream {

        public BlockingFilterInputStream(InputStream input) {
            super(input);
        }

        @Override
        public int read(byte[] buffer, int offset, int count) throws IOException {
            int total = 0;
            while (total < count) {
                int read = super.read(buffer, offset + total, count - total);
                if (read == -1) {
                    return (total != 0) ? total : -1;
                }
                total += read;
            }
            return total;
        }

        @Override
        public int read(byte[] buffer) throws IOException {
            int total = 0;
            while (total < buffer.length) {
                int offset = total;
                int count = buffer.length - total;
                int read = super.read(buffer, offset, count);
                if (read == -1) {
                    return (total != 0) ? total : -1;
                }
                total += read;
            }
            return total;
        }

        @Override
        public long skip(long count) throws IOException {
            long total = 0L;
            while (total < count) {
                long skipped = super.skip(count - total);
                if (skipped == 0L) {
                    int b = super.read();
                    if (b < 0) {
                        break;
                    } else {
                        skipped += 1;
                    }
                }
                total += skipped;
            }
            return total;
        }
    }
}
