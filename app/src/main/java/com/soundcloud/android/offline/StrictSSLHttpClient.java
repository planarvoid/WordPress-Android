package com.soundcloud.android.offline;

import static com.soundcloud.android.offline.OfflineContentService.TAG;

import com.google.common.net.HttpHeaders;
import com.soundcloud.android.api.oauth.OAuth;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.Log;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

class StrictSSLHttpClient {

    private static final int READ_TIMEOUT_IN_SECOND = 10;

    private final OkHttpClient httpClient;
    private final OAuth oAuth;
    private final DeviceHelper deviceHelper;

    @Inject
    public StrictSSLHttpClient(@Named(OfflineModule.STRICT_SSL_CLIENT) OkHttpClient client, DeviceHelper helper, OAuth oAuth) {
        this.httpClient = configureClient(client);
        this.deviceHelper = helper;
        this.oAuth = oAuth;
    }

    private OkHttpClient configureClient(OkHttpClient client) {
        client.setReadTimeout(READ_TIMEOUT_IN_SECOND, TimeUnit.SECONDS);
        return client;
    }

    public TrackFileResponse getFileStream(String fileUrl) throws IOException {
        final Request request = new Request.Builder()
                .url(fileUrl)
                .addHeader(HttpHeaders.USER_AGENT, deviceHelper.getUserAgent())
                .addHeader(HttpHeaders.AUTHORIZATION, oAuth.getAuthorizationHeaderValue())
                .build();
        logRequest(request);

        final Response response = httpClient.newCall(request).execute();
        logResponse(response);
        return new TrackFileResponse(response);
    }

    private void logRequest(Request request) {
        Log.d(TAG, "[OkHttp] " + request.method() + " " + request.urlString() + "; headers = " + request.headers());
    }

    private void logResponse(Response response) {
        Log.d(TAG, "[OkHttp] " + response);
    }

    static class TrackFileResponse implements Closeable {
        private final Response response;

        public TrackFileResponse(Response response) {
            this.response = response;
        }

        public boolean isSuccess() {
            return response.isSuccessful();
        }

        public boolean isFailure() {
            return !response.isSuccessful();
        }

        public boolean isUnavailable() {
            return response.code() >= 400 && response.code() <= 499;
        }

        public InputStream getInputStream() throws IOException {
            return response.body().byteStream();
        }

        public void close() {
            IOUtils.close(response.body());
        }
    }
}
