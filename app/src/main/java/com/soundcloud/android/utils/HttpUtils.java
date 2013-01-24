package com.soundcloud.android.utils;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.net.Uri;
import android.net.http.AndroidHttpClient;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public final class HttpUtils {
    private HttpUtils() {}

    public static void fetchUriToFile(String url, File file, boolean useCache) throws FileNotFoundException {
        OutputStream os = null;
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setUseCaches(useCache);
            conn.connect();
            final int status = conn.getResponseCode();
            if (status == HttpStatus.SC_OK) {
                InputStream is = conn.getInputStream();
                os = new BufferedOutputStream(new FileOutputStream(file));
                final byte[] buffer = new byte[8192];
                int n;
                while ((n = is.read(buffer, 0, buffer.length)) != -1) {
                    os.write(buffer, 0, n);
                }
            } else {
                throw new FileNotFoundException("HttpStatus: "+status);
            }
        } catch (MalformedURLException e) {
            throw new FileNotFoundException(e.getMessage());
        } catch (IOException e) {
            IOUtils.deleteFile(file);
            throw new FileNotFoundException(e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
            if (os != null) try {
                os.close();
            } catch (IOException ignored) {
            }
        }
    }

    public static HttpClient createHttpClient(String userAgent) {
        return AndroidHttpClient.newInstance(userAgent);
    }

    public static void closeHttpClient(HttpClient client) {
        if (client instanceof AndroidHttpClient) {
            // avoid leak error logging
            ((AndroidHttpClient) client).close();
        } else if (client != null) {
            client.getConnectionManager().shutdown();
        }
    }

    public static @Nullable Uri getRedirectUri(@NotNull HttpClient client, @NotNull Uri target) {
        try {
            HttpGet get = new HttpGet(target.toString());
            HttpResponse resp = client.execute(get);
            if (HttpStatus.SC_MOVED_TEMPORARILY == resp.getStatusLine().getStatusCode()) {
                Header location = resp.getFirstHeader("Location");
                if (location != null && !TextUtils.isEmpty(location.getValue())) {
                    return Uri.parse(location.getValue());
                }
            } else {
                Log.w(TAG, "invalid status "+resp.getStatusLine());
            }
        } catch (IOException e) {
            Log.w(TAG, e);
        }
        return null;
    }
}
