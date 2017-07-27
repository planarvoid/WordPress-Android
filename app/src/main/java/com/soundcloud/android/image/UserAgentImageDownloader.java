package com.soundcloud.android.image;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.nostra13.universalimageloader.core.download.BaseImageDownloader;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.java.net.HttpHeaders;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;

@AutoFactory(allowSubclasses = true)
public class UserAgentImageDownloader extends BaseImageDownloader {

    private final OkHttpClient okHttpClient;
    private final String userAgent;

    public UserAgentImageDownloader(Context context,
                                    @Provided OkHttpClient okHttpClient,
                                    @Provided DeviceHelper deviceHelper) {
        super(context);
        this.okHttpClient = okHttpClient;
        this.userAgent = deviceHelper.getUserAgent();
    }

    @Override
    protected InputStream getStreamFromNetwork(String imageUri, Object extra) throws IOException {

        final Request request = new Request.Builder()
                .url(imageUri)
                .addHeader(HttpHeaders.USER_AGENT, userAgent)
                .build();

        return okHttpClient.newCall(request).execute().body().byteStream();
    }
}
