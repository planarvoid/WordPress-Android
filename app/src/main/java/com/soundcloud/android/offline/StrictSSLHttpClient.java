package com.soundcloud.android.offline;

import static com.soundcloud.android.offline.OfflineContentService.TAG;

import com.google.common.net.HttpHeaders;
import com.soundcloud.android.api.oauth.OAuth;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.android.utils.Log;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.InputStream;

class StrictSSLHttpClient {

    private final OkHttpClient httpClient;
    private final OAuth oAuth;
    private final DeviceHelper deviceHelper;

    @Inject
    public StrictSSLHttpClient(@Named("StrictSSLHttpClient") OkHttpClient client, DeviceHelper helper, OAuth oAuth) {
        this.httpClient = client;
        this.deviceHelper = helper;
        this.oAuth = oAuth;
    }

    public InputStream downloadFile(String fileUrl) throws IOException {
        final Request request = new Request.Builder()
                .url(fileUrl)
                .addHeader(HttpHeaders.USER_AGENT, deviceHelper.getUserAgent())
                .addHeader(HttpHeaders.AUTHORIZATION, oAuth.getAuthorizationHeaderValue())
                .build();
        logRequest(request);

        final Response response = httpClient.newCall(request).execute();
        logResponse(response);
        return response.body().byteStream();
    }

    private void logRequest(Request request) {
        Log.d(TAG, "[OkHttp] " + request.method() + " " + request.urlString() + "; headers = " + request.headers());
    }

    private void logResponse(Response response) {
        Log.d(TAG, "[OkHttp] " + response);
    }

}
