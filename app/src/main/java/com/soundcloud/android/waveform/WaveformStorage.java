package com.soundcloud.android.waveform;

import static com.soundcloud.android.storage.StorageModule.WAVEFORM_CACHE;

import com.soundcloud.android.model.Urn;
import com.vincentbrison.openlibraries.android.dualcache.DualCache;
import io.reactivex.Maybe;

import javax.inject.Inject;
import javax.inject.Named;

public class WaveformStorage {

    private final DualCache<WaveformData> waveformCache;

    @Inject
    public WaveformStorage(@Named(WAVEFORM_CACHE) DualCache<WaveformData> waveformCache) {
        this.waveformCache = waveformCache;
    }

    Maybe<WaveformData> waveformData(Urn trackUrn) {
        return Maybe.fromCallable(() -> waveformCache.get(getKey(trackUrn)));
    }

    public boolean isWaveformStored(Urn trackUrn) {
        return waveformCache.contains(getKey(trackUrn));
    }

    public void store(Urn trackUrn, WaveformData data) {
        waveformCache.put(getKey(trackUrn), data);
    }

    public void clear() {
        waveformCache.invalidate();
    }

    private String getKey(Urn trackUrn) {
        return trackUrn.getStringId();
    }
}
