package com.soundcloud.android.waveform;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TrackUrn;
import com.soundcloud.android.model.WaveformData;
import com.soundcloud.android.playback.ui.view.WaveformView;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

import android.support.v4.util.LruCache;

import javax.inject.Inject;
import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

public class WaveformOperations {

    public static final int DEFAULT_WAVEFORM_CACHE_SIZE = 20;

    private final LruCache<TrackUrn, WaveformData> waveformCache;
    private final WaveformFetcher waveformFetcher;
    private final WeakHashMap<WaveformView, TrackUrn> viewToTrackMap = new WeakHashMap<WaveformView, TrackUrn>();

    @Inject
    public WaveformOperations(LruCache<TrackUrn, WaveformData> waveformCache, WaveformFetcher waveformFetcher) {
        this.waveformCache = waveformCache;
        this.waveformFetcher = waveformFetcher;
    }

    @Deprecated
    public Observable<WaveformResult> waveformFor(final Track track){
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

    public void display(final TrackUrn trackUrn, String waveformUrl, WaveformView waveformView) {
        viewToTrackMap.put(waveformView, trackUrn);

        final WaveformData cachedWaveform = waveformCache.get(trackUrn);
        if (cachedWaveform != null){
            waveformView.setWaveform(cachedWaveform);
        } else {
            final WeakReference<WaveformView> weakReference = new WeakReference<WaveformView>(waveformView);
            waveformFetcher.fetch(waveformUrl).doOnNext(new Action1<WaveformData>() {
                @Override
                public void call(WaveformData waveformData) {
                    waveformCache.put(trackUrn, waveformData);
                }
            }).onErrorResumeNext(waveformFetcher.fetchDefault()).subscribe(new DefaultSubscriber<WaveformData>() {
                @Override
                public void onNext(WaveformData waveformData) {
                    WaveformView waveformView =  weakReference.get();
                    if (waveformView != null && trackUrn.equals(viewToTrackMap.get(waveformView))) {
                        waveformView.setWaveform(waveformData);
                    }
                }
            });
        }
    }
}
