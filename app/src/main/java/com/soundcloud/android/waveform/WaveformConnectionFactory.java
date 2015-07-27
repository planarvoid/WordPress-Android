package com.soundcloud.android.waveform;

import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

class WaveformConnectionFactory {

    private static final int CONNECT_TIMEOUT = 3000;
    private static final int READ_TIMEOUT = 3000;

    @Inject
    WaveformConnectionFactory() {
    }

    HttpURLConnection create(String waveformUrl) throws IOException {
        return configure((HttpURLConnection) new URL(waveformUrl).openConnection());
    }

    @VisibleForTesting
    HttpURLConnection configure(HttpURLConnection connection) {
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);
        connection.setUseCaches(true);
        return connection;
    }
}
