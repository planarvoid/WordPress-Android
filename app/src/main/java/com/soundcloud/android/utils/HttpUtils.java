package com.soundcloud.android.utils;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;

import android.net.http.AndroidHttpClient;

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
}
