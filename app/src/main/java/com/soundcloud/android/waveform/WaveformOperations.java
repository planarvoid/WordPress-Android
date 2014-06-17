package com.soundcloud.android.waveform;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TrackUrn;
import com.soundcloud.android.model.WaveformData;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

import android.support.v4.util.LruCache;

import javax.inject.Inject;

public class WaveformOperations {

    public static final int DEFAULT_WAVEFORM_CACHE_SIZE = 20;

    private final LruCache<TrackUrn, WaveformData> waveformCache;
    private final WaveformFetcher waveformFetcher;

    @Inject
    public WaveformOperations(LruCache<TrackUrn, WaveformData> waveformCache, WaveformFetcher waveformFetcher) {
        this.waveformCache = waveformCache;
        this.waveformFetcher = waveformFetcher;
    }

    public Observable<WaveformResult> waveformDataFor(final Track track){
        final TrackUrn trackUrn = track.getUrn();
        final WaveformData cachedWaveform = waveformCache.get(trackUrn);
        if (cachedWaveform != null){
            return Observable.just(WaveformResult.fromCache(cachedWaveform));
        } else {
            return waveformFetcher.fetch(track.getWaveformUrl()).doOnNext(new Action1<WaveformData>() {
                @Override
                public void call(WaveformData waveformData) {
                    waveformCache.put(trackUrn, waveformData);
                }
            }).map(new Func1<WaveformData, WaveformResult>() {
                @Override
                public WaveformResult call(WaveformData waveformData) {
                    return WaveformResult.fromNetwork(waveformData);
                }
            }).onErrorResumeNext(waveformFetcher.fetchDefault().map(new Func1<WaveformData, WaveformResult>() {
                @Override
                public WaveformResult call(WaveformData waveformData) {
                    return WaveformResult.fromError(waveformData);
                }
            }));
        }
    }

}
