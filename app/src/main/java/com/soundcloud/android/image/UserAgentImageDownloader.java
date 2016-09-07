package com.soundcloud.android.image;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.nostra13.universalimageloader.core.download.BaseImageDownloader;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.java.net.HttpHeaders;

import android.content.Context;

import java.io.IOException;
import java.net.HttpURLConnection;

@AutoFactory(allowSubclasses = true)
public class UserAgentImageDownloader extends BaseImageDownloader {

    private final String userAgent;

    public UserAgentImageDownloader(Context context,
                                    @Provided DeviceHelper deviceHelper) {
        super(context);
        this.userAgent = deviceHelper.getUserAgent();
    }

    @Override
    protected HttpURLConnection createConnection(String url, Object extra) throws IOException {
        HttpURLConnection conn = super.createConnection(url, extra);
        conn.setRequestProperty(HttpHeaders.USER_AGENT, userAgent);
        return conn;
    }
}
