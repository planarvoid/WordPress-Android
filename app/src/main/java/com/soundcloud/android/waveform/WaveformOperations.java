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

    /*
     * TODO: This is just for working on the new player UI. We can remove it once we're fetching real waveforms again!
     */
    public WaveformData getDefaultWaveform() {
        return new WaveformData(140, new int[] { 83,91,92,92,91,94,93,87,91,88,90,90,85,82,83,85,94,96,99,98,96,94,92,91,89,89,89,91,96,94,92,94,94,94,92,97,93,91,90,86,83,82,79,79,76,76,91,91,85,92,86,87,88,88,88,87,89,86,88,90,89,89,91,95,93,93,93,91,90,89,92,91,88,85,81,83,83,93,97,99,99,95,96,94,92,89,90,91,90,92,94,91,93,93,94,94,96,93,91,92,85,84,85,80,81,79,77,84,94,93,90,86,87,88,89,91,89,87,86,85,89,90,89,90,92,92,93,91,94,93,88,92,89,91,91,84,83,84,87,95,97,100,98,96,95,92,91,89,90,89,92,97,94,92,95,95,94,94,97,93,93,92,88,86,84,81,81,79,81,93,92,87,92,86,87,88,89,88,88,89,86,88,90,88,89,92,96,94,93,94,93,90,90,92,91,88,85,82,85,84,94,97,99,99,95,96,94,91,89,90,91,91,92,93,91,93,94,95,94,96,94,92,93,88,86,85,81,82,80,80,87,95,93,95,90,91,90,90,92,90,87,87,86,89,90,89,127,122,130,128,104,94,106,125,126,125,130,128,88,82,92,117,119,127,127,118,94,95,122,122,119,130,128,106,96,96,110,124,125,129,127,98,93,114,125,123,129,129,122,78,75,93,124,122,129,128,106,88,100,125,123,125,130,127,94,89,94,117,122,128,130,121,91,92,121,127,122,129,127,113,80,85,104,124,123,128,126,99,96,112,125,116,128,129,121,92,92,98,123,122,128,128,114,90,93,126,127,123,132,131,95,75,86,93,93,90,90,86,87,88,90,91,90,91,91,96,98,96,92,131,132,136,135,102,97,109,127,129,131,133,127,93,95,102,124,126,133,131,118,95,98,118,126,127,134,133,107,100,98,112,128,135,135,130,98,97,119,127,129,135,133,119,87,87,98,127,131,134,132,105,93,102,124,124,132,135,130,96,94,95,121,128,132,131,121,93,98,123,130,129,137,136,111,100,104,112,125,133,133,129,99,98,113,126,119,134,133,123,98,94,99,126,131,136,135,112,95,106,125,129,130,132,130,102,103,104,117,128,135,134,124,90,92,118,128,126,134,133,113,96,93,99,102,102,97,99,101,98,94,95,97,93,94,94,91,91,89,92,96,101,101,105,100,94,98,97,98,95,95,97,100,99,98,96,98,96,99,99,95,95,95,97,95,93,94,92,95,92,96,92,93,92,87,90,92,96,93,95,93,94,91,95,97,95,98,100,101,98,97,96,98,95,97,96,99,97,92,97,94,98,100,103,101,103,100,100,99,99,100,98,102,100,99,98,95,95,99,97,101,96,96,101,98,98,100,97,97,98,100,95,93,93,93,92,90,88,98,99,96,92,95,94,95,97,113,128,133,136,132,100,100,118,129,127,134,134,124,99,103,111,129,124,133,131,112,96,105,125,124,128,133,130,109,109,107,122,128,136,136,126,98,100,125,130,126,135,133,116,97,98,110,128,130,136,133,104,94,111,124,126,132,138,128,104,107,105,126,129,135,135,116,93,100,129,132,126,137,134,105,98,104,117,125,133,133,125,98,97,118,126,122,134,133,121,107,104,106,131,130,136,134,107,98,111,128,129,130,133,127,101,100,105,123,127,135,134,120,102,98,123,130,125,136,135,115,105,106,116,129,135,138,131,99,96,118,130,127,131,133,120,91,95,100,128,127,134,133,109,97,109,127,124,128,134,130,109,107,105,122,128,132,132,124,110,109,128,133,126,138,137,113,103,105,114,127,130,133,129,108,104,117,126,125,132,137,125,101,107,107,128,129,135,133,113,100,108,128,129,129,135,133,99,94,98,117,125,134,134,124,98,102,120,127,124,133,132,119,108,107,112,128,132,136,132,108,102,117,128,128,130,132,126,106,110,111,127,128,136,136,117,98,96,125,128,126,136,135,106,103,108,119,128,134,133,128,96,95,121,129,126,135,135,117,91,95,101,128,128,133,131,104,97,108,126,123,129,132,127,108,107,104,124,127,133,131,118,96,98,127,132,125,138,136,108,94,99,115,127,132,132,128,98,93,116,125,124,133,135,123,102,105,105,130,130,137,135,110,96,110,128,128,129,133,131,104,103,105,121,124,132,132,120,97,97,121,127,123,133,131,114,107,105,114,128,132,136,131,101,102,117,129,127,133,134,125,105,107,106,127,128,136,134,112,97,104,125,127,127,135,131,106,105,113,109,100,98,94,107,86,78,79,105,86,91,91,107,72,79,86,103,87,95,101,99,78,79,99,96,90,91,105,92,89,86,107,89,92,92,107,78,82,89,105,89,92,100,99,73,80,96,97,93,94,106,91,90,83,105,92,93,92,108,88,94,92,108,87,94,101,101,81,84,98,100,90,94,105,93,83,86,107,93,99,96,110,90,88,88,110,92,94,99,106,93,100,103,104,93,95,106,96,86,93,109,96,96,93,108,87,89,89,111,98,100,109,108,102,104,108,111,111,100,109,109,107,108,124,139,137,137,138,139,137,136,139,133,134,132,132,133,131,132,123,104,111,111,108,108,120,134,131,135,136,136,136,129,109,133,139,139,139,138,137,138,138,134,135,134,133,133,133,131,128,114,103,105,106,103,108,125,131,103,106,101,107,109,112,117,133,133,132,133,132,132,134,135,132,133,134,133,132,132,131,131,105,109,104,107,100,105,131,132,135,133,135,134,134,115,123,136,135,133,134,135,135,136,138,133,134,134,134,133,132,127,127,101,106,107,104,108,114,131,121,107,98,102,107,111,109,127,138,136,134,134,133,133,137,133,130,130,128,130,128,130,129,121,104,107,109,104,103,120,132,130,131,132,133,131,123,111,137,139,139,139,138,136,137,136,133,134,133,130,131,130,130,127,113,103,105,105,102,109,126,131,98,107,101,108,109,112,119,136,137,138,137,138,138,138,139,134,135,134,134,132,132,131,131,106,112,107,109,107,113,130,130,136,132,133,133,131,116,125,138,139,138,138,138,138,136,139,134,134,134,134,132,131,124,107,104,104,108,104,110,107,104,99,94,96,99,104,111,107,130,129,133,132,129,104,111,128,133,130,135,135,122,100,110,121,126,133,133,132,113,107,123,128,124,135,134,131,113,114,113,134,129,134,134,122,103,114,130,131,132,135,134,113,98,105,127,127,134,132,131,105,111,127,133,129,134,134,125,110,115,120,131,132,135,133,114,103,122,131,129,132,132,131,106,104,107,131,126,132,134,123,106,115,128,125,131,132,134,121,111,113,126,128,133,132,130,106,109,127,134,128,135,136,127,102,108,117,129,130,133,133,117,110,122,130,127,134,133,133,112,113,112,132,130,133,134,128,105,113,128,131,131,133,133,121,105,114,123,125,133,132,133,111,106,123,128,126,133,131,130,112,119,122,131,131,134,133,120,102,118,130,130,132,133,132,114,106,111,129,127,135,134,131,110,112,129,131,129,135,135,125,115,119,125,129,134,134,133,114,106,125,131,127,131,134,130,109,113,113,132,128,133,132,124,109,116,129,125,130,131,132,124,117,118,129,128,133,131,130,109,103,127,134,129,134,134,126,111,118,125,130,132,133,133,119,109,124,131,128,135,134,133,114,113,114,107,108,108,107,106,106,102,100,100,98,99,99,94,93,92,97,101,105,105,102,100,99,97,96,94,93,92,96,101,98,95,99,100,99,98,100,97,96,93,88,87,85,82,83,80,81,96,92,88,92,87,88,87,87,85,85,84,82,83,84,82,84,85,87,84,84,84,82,78,80,79,78,75,73,70,70,69,77,78,80,78,75,75,73,71,68,68,68,68,69,68,66,67,67,66,66,68,64,63,63,58,56,56,52,53,50,50,56,59,57,57,53,55,54,55,55,53,51,51,50,52,52,51,52,52,51,50,50,52,49,47,48,46,47,46,42,41,41,44,47,47,48,46,45,43,42,41,39,38,38,39,40,38,36,37,36,35,34,34,32,31,29,27,26,24,23,22,21,21,25,23,22,22,20,20,19,19,18,18,17,16,16,16,15,15,15,14,14,14,14,14,13,13,14,13,13,12,12,12,12,14,13 });
    }

}
