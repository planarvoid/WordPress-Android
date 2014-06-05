package com.soundcloud.android.waveform;

import com.soundcloud.android.model.WaveformData;

public class WaveformResult {
    private WaveformData waveformData;
    private Source source;

    private static enum Source {
        CACHE, NETWORK, ERROR
    }

    WaveformResult(WaveformData waveformData, Source source) {
        this.waveformData = waveformData;
        this.source = source;
    }

    public static WaveformResult fromCache(WaveformData waveformData){
        return new WaveformResult(waveformData, Source.CACHE);
    }

    public static WaveformResult fromNetwork(WaveformData waveformData){
        return new WaveformResult(waveformData, Source.NETWORK);
    }

    public static WaveformResult fromError(WaveformData waveformData){
        return new WaveformResult(waveformData, Source.ERROR);
    }

    public WaveformData getWaveformData() {
        return waveformData;
    }

    public boolean isFromCache() {
        return source == Source.CACHE;
    }
}
