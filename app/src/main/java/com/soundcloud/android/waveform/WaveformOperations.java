package com.soundcloud.android.waveform;

import com.soundcloud.android.model.Urn;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;

import android.support.v4.util.LruCache;

import javax.inject.Inject;

public class WaveformOperations {

    public static final int DEFAULT_WAVEFORM_CACHE_SIZE = 20;

    private final LruCache<Urn, WaveformData> waveformCache;
    private final WaveformFetcher waveformFetcher;

    @Inject
    WaveformOperations(LruCache<Urn, WaveformData> waveformCache, WaveformFetcher waveformFetcher) {
        this.waveformCache = waveformCache;
        this.waveformFetcher = waveformFetcher;
    }

    public Observable<WaveformData> waveformDataFor(final Urn trackUrn, final String waveformUrl) {
        return Observable.create(new Observable.OnSubscribe<WaveformData>() {
            @Override
            public void call(Subscriber<? super WaveformData> subscriber) {
                subscriber.onNext(waveformCache.get(trackUrn));
                subscriber.onCompleted();
            }
        }).flatMap(new Func1<WaveformData, Observable<WaveformData>>() {
            @Override
            public Observable<WaveformData> call(WaveformData waveformData) {
                if (waveformData != null) {
                    return Observable.just(waveformData);
                }

                if (waveformUrl == null) {
                    return waveformFetcher.fetchDefault();
                }

                return waveformFetcher.fetch(waveformUrl).doOnNext(new Action1<WaveformData>() {
                    @Override
                    public void call(WaveformData waveformData) {
                        waveformCache.put(trackUrn, waveformData);
                    }
                });
            }
        }).onErrorResumeNext(waveformFetcher.fetchDefault());
    }
}
