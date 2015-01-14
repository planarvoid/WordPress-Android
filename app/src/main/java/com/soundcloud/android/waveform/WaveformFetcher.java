package com.soundcloud.android.waveform;

import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.ScheduledOperations;
import com.soundcloud.android.utils.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;

import android.content.Context;
import android.net.Uri;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

class WaveformFetcher extends ScheduledOperations {

    private static final String DEFAULT_WAVEFORM_ASSET_FILE = "default_waveform.json";
    private static final String WAVEFORM_URL_PREFIX = "http://wis.sndcdn.com/%s";
    private static final String WIDTH = "width";
    private static final String HEIGHT = "height";
    private static final String SAMPLES = "samples";
    private final WaveformConnectionFactory waveformConnectionFactory;
    private final Context context;

    @Inject
    WaveformFetcher(Context context, WaveformConnectionFactory waveformConnectionFactory) {
        this(ScSchedulers.API_SCHEDULER, context, waveformConnectionFactory);
    }

    WaveformFetcher(Scheduler subscribeOn, Context context,
                           WaveformConnectionFactory waveformConnectionFactory) {
        super(subscribeOn);
        this.context = context;
        this.waveformConnectionFactory = waveformConnectionFactory;
    }

    Observable<WaveformData> fetch(final String waveformUrl) {
        return schedule(Observable.create(new Observable.OnSubscribe<WaveformData>() {
            @Override
            public void call(Subscriber<? super WaveformData> subscriber) {
                try {
                    final String waveformApiUrl = transformWaveformUrl(waveformUrl);
                    final HttpURLConnection connection = waveformConnectionFactory.create(waveformApiUrl);
                    final int code = connection.getResponseCode();
                    if (code == HttpURLConnection.HTTP_OK) {
                        subscriber.onNext(parseWaveformData(connection.getInputStream()));
                        subscriber.onCompleted();
                    } else {
                        subscriber.onError(new IOException("invalid status code received: " + code));
                    }
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        }));
    }

    Observable<WaveformData> fetchDefault() {
        return schedule(Observable.create(new Observable.OnSubscribe<WaveformData>() {
            @Override
            public void call(Subscriber<? super WaveformData> subscriber) {
                try {
                    subscriber.onNext(parseWaveformData(context.getAssets().open(DEFAULT_WAVEFORM_ASSET_FILE)));
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        }));
    }

    private static String transformWaveformUrl(String waveformUrl){
        return String.format(WAVEFORM_URL_PREFIX, Uri.parse(waveformUrl).getLastPathSegment());
    }

    private static WaveformData parseWaveformData(InputStream data) throws JSONException, IOException {
        final JSONObject obj = new JSONObject(IOUtils.readInputStream(data));
        final int width = obj.getInt(WIDTH);
        final int height = obj.getInt(HEIGHT);
        final int[] samples = new int[width];
        final JSONArray sampleArray = obj.getJSONArray(SAMPLES);

        if (sampleArray == null || sampleArray.length() == 0) {
            throw new IOException("no samples provided");
        }

        if (sampleArray.length() != width) {
            throw new IOException("incomplete sample data");
        }

        for (int i = 0; i < width; i++) {
            double value =  Math.pow(sampleArray.getDouble(i) / height, 1.5);
            samples[i] = (int) (height * value);
        }

        return new WaveformData(height, samples);
    }
}
