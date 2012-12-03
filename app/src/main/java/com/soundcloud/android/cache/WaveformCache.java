package com.soundcloud.android.cache;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.WaveformData;
import com.soundcloud.android.task.fetch.WaveformFetcher;

import android.support.v4.util.LruCache;

public final class WaveformCache {

    private static WaveformCache sInstance;

    private android.support.v4.util.LruCache<Track, WaveformData> mCache
            = new LruCache<Track, WaveformData>(128);

    private WaveformCache() {
    }

    public static synchronized WaveformCache get() {
        if (sInstance == null) {
            sInstance = new WaveformCache();
        }
        return sInstance;
    }

    public WaveformData getData(final Track track, final WaveformCallback callback) {
        WaveformData data = mCache.get(track);
        if (data != null) {
            callback.onWaveformDataLoaded(data);
            return data;
        } else {
            new WaveformFetcher() {
                @Override
                protected void onPostExecute(WaveformData waveformData) {
                    if (waveformData != null) {
                        mCache.put(track, waveformData);
                        callback.onWaveformDataLoaded(waveformData);
                    } else {
                        callback.onWaveformError();
                    }
                }
            }.executeOnThreadPool(track.getWaveformDataURL());
        }
        return null;
    }

    public interface WaveformCallback {
        void onWaveformDataLoaded(WaveformData data);
        void onWaveformError();
    }
}
