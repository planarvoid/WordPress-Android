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

package com.google.android.imageloader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.ContentHandler;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * A {@link ContentHandler} that decodes a {@link Bitmap} from a
 * {@link URLConnection}.
 * <p>
 * The implementation includes a work-around for <a
 * href="http://code.google.com/p/android/issues/detail?id=6066">Issue 6066</a>.
 * <p>
 * An {@link IOException} is thrown if there is a decoding exception.
 */
public class BitmapContentHandler extends ContentHandler {
    public static final String TAG = BitmapContentHandler.class.getSimpleName();

    public static final int READ_TIMEOUT    = 10000;
    public static final int CONNECT_TIMEOUT = 3000;
    private static final int LOADTIME_WARN  = 10 * 1000; // flag requests taking longer than 10 sec

    @Override public Bitmap getContent(URLConnection connection) throws IOException {
        connection.setRequestProperty("Accept-Encoding", "identity");
        connection.setRequestProperty("Connection", "Close");
        connection.setReadTimeout(READ_TIMEOUT);
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setUseCaches(true);

        final long start = System.currentTimeMillis();
        final URL url = connection.getURL();
        try {
            InputStream input = new BlockingFilterInputStream(connection.getInputStream());
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            if (bitmap == null) {
                throw new IOException("Image could not be decoded");
            } else {
                return bitmap;
            }
        } finally {
            final long loadTime = System.currentTimeMillis() - start;
            if (loadTime > LOADTIME_WARN) {
                Log.w(TAG, "slow image loading request " + loadTime + " ms, url = " + url);
            } else if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "image fetched in " + loadTime + " ms, url = " + url);
            }
            // should only be needed for Keep-Alive which we're not using
            if (connection instanceof HttpURLConnection) {
                ((HttpURLConnection)connection).disconnect();
            }
        }
    }
}
