package com.soundcloud.android.waveform;

import com.soundcloud.android.model.Urn;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

import android.support.v4.util.LruCache;

import javax.inject.Inject;

public class WaveformOperations {

    public static final int DEFAULT_WAVEFORM_CACHE_SIZE = 20;

    private final LruCache<Urn, WaveformData> waveformCache;
    private final WaveformFetcher waveformFetcher;

    @Inject
    public WaveformOperations(LruCache<Urn, WaveformData> waveformCache, WaveformFetcher waveformFetcher) {
        this.waveformCache = waveformCache;
        this.waveformFetcher = waveformFetcher;
    }

    public Observable<WaveformResult> waveformDataFor(final Urn trackUrn, final String waveformUrl) {
        final WaveformData cachedWaveform = waveformCache.get(trackUrn);
        if (cachedWaveform == null) {
            return waveformFetcher.fetch(waveformUrl).doOnNext(new Action1<WaveformData>() {
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
        } else {
            return Observable.just(WaveformResult.fromCache(cachedWaveform));
        }
    }

}
