package com.soundcloud.android.cache;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.WaveformData;
import com.soundcloud.android.playback.WaveformFetcher;

import android.support.v4.util.LruCache;

public final class WaveformCache {

    public static final int MAX_CACHE_SIZE = 20;
    private static WaveformCache instance;

    private android.support.v4.util.LruCache<Long, WaveformData> cache
            = new LruCache<Long, WaveformData>(MAX_CACHE_SIZE);

    private WaveformCache() {
    }

    public static synchronized WaveformCache get() {
        if (instance == null) {
            instance = new WaveformCache();
        }
        return instance;
    }

    public WaveformData getData(final Track track, final WaveformCallback callback) {
        WaveformData data = cache.get(track.getId());
        if (data != null) {
            callback.onWaveformDataLoaded(track, data, true);
            return data;
        } else {
            new WaveformFetcher() {
                @Override
                protected void onPostExecute(WaveformData waveformData) {
                    if (waveformData != null) {
                        cache.put(track.getId(), waveformData);
                        callback.onWaveformDataLoaded(track, waveformData, false);
                    } else {
                        callback.onWaveformError(track);
                    }
                }
            }.executeOnThreadPool(track.getWaveformDataURL());
        }
        return null;
    }

    public interface WaveformCallback {
        void onWaveformDataLoaded(Track track, WaveformData data, boolean fromCache);
        void onWaveformError(Track track);
    }
}
