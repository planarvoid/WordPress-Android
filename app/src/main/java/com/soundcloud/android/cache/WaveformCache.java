package com.soundcloud.android.cache;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.WaveformData;
import com.soundcloud.android.playback.WaveformFetcher;

import android.support.v4.util.LruCache;

public final class WaveformCache {

    public static final int MAX_CACHE_SIZE = 20;
    private static WaveformCache sInstance;

    private android.support.v4.util.LruCache<Long, WaveformData> mCache
            = new LruCache<Long, WaveformData>(MAX_CACHE_SIZE);

    private WaveformCache() {
    }

    public static synchronized WaveformCache get() {
        if (sInstance == null) {
            sInstance = new WaveformCache();
        }
        return sInstance;
    }

    public WaveformData getData(final Track track, final WaveformCallback callback) {
        WaveformData data = mCache.get(track.getId());
        if (data != null) {
            callback.onWaveformDataLoaded(track, data, true);
            return data;
        } else {
            new WaveformFetcher() {
                @Override
                protected void onPostExecute(WaveformData waveformData) {
                    if (waveformData != null) {
                        mCache.put(track.getId(), waveformData);
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
