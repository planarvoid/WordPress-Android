package com.soundcloud.android.waveform;

import com.soundcloud.android.commands.Command;
import org.json.JSONException;

import android.net.Uri;

import javax.inject.Inject;
import java.io.IOException;
import java.net.HttpURLConnection;

public class WaveformFetchCommand extends Command<String, WaveformData> {

    private static final String WAVEFORM_URL_PREFIX = "http://wis.sndcdn.com/%s";

    private final WaveformConnectionFactory waveformConnectionFactory;
    private final WaveformParser waveformParser;

    @Inject
    WaveformFetchCommand(WaveformConnectionFactory waveformConnectionFactory, WaveformParser waveformParser) {
        this.waveformConnectionFactory = waveformConnectionFactory;
        this.waveformParser = waveformParser;
    }

    @Override
    public WaveformData call(String input) {
        final String waveformApiUrl = transformWaveformUrl(input);
        final HttpURLConnection connection;
        try {
            connection = waveformConnectionFactory.create(waveformApiUrl);
            final int code = connection.getResponseCode();
            if (code == HttpURLConnection.HTTP_OK) {
                return waveformParser.parse(connection.getInputStream());
            } else {
                throw new WaveformFetchException("Invalid response code");
            }
        } catch (IOException | JSONException e) {
            throw new WaveformFetchException(e);
        }
    }

    private static String transformWaveformUrl(String waveformUrl) {
        if (waveformUrl == null) {
            throw new IllegalStateException("Waveform URL is null");
        }
        return String.format(WAVEFORM_URL_PREFIX, Uri.parse(waveformUrl).getLastPathSegment());
    }

    public class WaveformFetchException extends RuntimeException {

        public WaveformFetchException(String message) {
            super(message);
        }

        public WaveformFetchException(Throwable t) {
            super(t);
        }
    }

}
