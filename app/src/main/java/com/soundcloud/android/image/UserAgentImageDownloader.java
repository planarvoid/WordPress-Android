package com.soundcloud.android.image;

import com.nostra13.universalimageloader.core.download.BaseImageDownloader;
import com.nostra13.universalimageloader.core.download.ImageDownloader;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.java.net.HttpHeaders;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.content.Context;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;

public final class UserAgentImageDownloader extends BaseImageDownloader {

    private final OkHttpClient okHttpClient;
    private final String userAgent;

    UserAgentImageDownloader(Context context,
                                     OkHttpClient okHttpClient,
                                     DeviceHelper deviceHelper) {
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

    public static class Factory {

        private final OkHttpClient okHttpClient;
        private final DeviceHelper deviceHelper;

        @Inject
        public Factory(OkHttpClient okHttpClient, DeviceHelper deviceHelper) {
            this.okHttpClient = okHttpClient;
            this.deviceHelper = deviceHelper;
        }

        @Nullable
        public ImageDownloader create(@NotNull Context context) {
            return new UserAgentImageDownloader(context, okHttpClient, deviceHelper);
        }
    }
}
